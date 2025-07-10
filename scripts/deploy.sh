#!/bin/bash

# Flink Template项目部署脚本
# 用途：部署应用到Flink集群

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 默认配置
DEFAULT_FLINK_HOME="/opt/flink"
DEFAULT_JAR_PATH="target/flink-template-1.0.0.jar"
DEFAULT_JOB_NAME="Sensor Data Processing Job"
DEFAULT_PARALLELISM=2
DEFAULT_CONFIG_FILE="src/main/resources/application.yml"

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $1"
}

# 显示帮助信息
show_help() {
    cat << EOF
Flink Template 部署脚本

用法: $0 [选项] [命令]

命令:
  submit      提交作业到Flink集群 (默认)
  cancel      取消运行中的作业
  list        列出所有作业
  status      查看作业状态
  savepoint   创建保存点
  stop        停止作业

选项:
  --flink-home PATH     Flink安装目录 (默认: $DEFAULT_FLINK_HOME)
  --jar-path PATH       JAR文件路径 (默认: $DEFAULT_JAR_PATH)
  --job-name NAME       作业名称 (默认: $DEFAULT_JOB_NAME)
  --parallelism NUM     并行度 (默认: $DEFAULT_PARALLELISM)
  --config-file PATH    配置文件路径 (默认: $DEFAULT_CONFIG_FILE)
  --job-id ID           作业ID (用于cancel/stop/savepoint命令)
  --savepoint-path PATH 保存点路径 (用于恢复作业)
  --env ENVIRONMENT     环境 (dev/test/prod, 默认: dev)
  --help, -h            显示帮助信息

示例:
  $0 submit --parallelism 4 --env prod
  $0 cancel --job-id d3c5e6f7-8a9b-1c2d-3e4f-5g6h7i8j9k0l
  $0 list
  $0 savepoint --job-id d3c5e6f7-8a9b-1c2d-3e4f-5g6h7i8j9k0l
EOF
}

# 检查Flink环境
check_flink() {
    if [ ! -d "$FLINK_HOME" ]; then
        log_error "Flink目录不存在: $FLINK_HOME"
        exit 1
    fi
    
    if [ ! -f "$FLINK_HOME/bin/flink" ]; then
        log_error "Flink命令不存在: $FLINK_HOME/bin/flink"
        exit 1
    fi
    
    log_info "Flink环境检查通过: $FLINK_HOME"
}

# 检查JAR文件
check_jar() {
    if [ ! -f "$JAR_PATH" ]; then
        log_error "JAR文件不存在: $JAR_PATH"
        log_info "请先运行构建脚本: ./scripts/build.sh"
        exit 1
    fi
    
    log_info "JAR文件检查通过: $JAR_PATH"
}

# 提交作业
submit_job() {
    log_info "提交Flink作业..."
    log_info "作业名称: $JOB_NAME"
    log_info "并行度: $PARALLELISM"
    log_info "环境: $ENVIRONMENT"
    log_info "JAR路径: $JAR_PATH"
    
    # 构建提交命令
    SUBMIT_CMD="$FLINK_HOME/bin/flink run"
    SUBMIT_CMD="$SUBMIT_CMD --parallelism $PARALLELISM"
    SUBMIT_CMD="$SUBMIT_CMD --jobmanager yarn-cluster"
    
    if [ -n "$SAVEPOINT_PATH" ]; then
        SUBMIT_CMD="$SUBMIT_CMD --fromSavepoint $SAVEPOINT_PATH"
        log_info "从保存点恢复: $SAVEPOINT_PATH"
    fi
    
    # 添加程序参数
    SUBMIT_CMD="$SUBMIT_CMD $JAR_PATH"
    SUBMIT_CMD="$SUBMIT_CMD --spring.profiles.active=$ENVIRONMENT"
    
    if [ -f "$CONFIG_FILE" ]; then
        SUBMIT_CMD="$SUBMIT_CMD --spring.config.location=file:$CONFIG_FILE"
    fi
    
    log_debug "执行命令: $SUBMIT_CMD"
    
    # 执行提交
    eval $SUBMIT_CMD
    
    if [ $? -eq 0 ]; then
        log_info "作业提交成功"
        
        # 等待一下再列出作业
        sleep 3
        list_jobs
    else
        log_error "作业提交失败"
        exit 1
    fi
}

# 取消作业
cancel_job() {
    if [ -z "$JOB_ID" ]; then
        log_error "请提供作业ID: --job-id <job-id>"
        exit 1
    fi
    
    log_info "取消作业: $JOB_ID"
    $FLINK_HOME/bin/flink cancel $JOB_ID
    
    if [ $? -eq 0 ]; then
        log_info "作业取消成功"
    else
        log_error "作业取消失败"
        exit 1
    fi
}

# 停止作业（优雅关闭）
stop_job() {
    if [ -z "$JOB_ID" ]; then
        log_error "请提供作业ID: --job-id <job-id>"
        exit 1
    fi
    
    log_info "停止作业: $JOB_ID"
    $FLINK_HOME/bin/flink stop $JOB_ID
    
    if [ $? -eq 0 ]; then
        log_info "作业停止成功"
    else
        log_error "作业停止失败"
        exit 1
    fi
}

# 列出作业
list_jobs() {
    log_info "当前运行的作业:"
    $FLINK_HOME/bin/flink list
}

# 查看作业状态
show_status() {
    if [ -n "$JOB_ID" ]; then
        log_info "作业状态: $JOB_ID"
        $FLINK_HOME/bin/flink info $JOB_ID
    else
        list_jobs
    fi
}

# 创建保存点
create_savepoint() {
    if [ -z "$JOB_ID" ]; then
        log_error "请提供作业ID: --job-id <job-id>"
        exit 1
    fi
    
    log_info "创建保存点: $JOB_ID"
    
    if [ -n "$SAVEPOINT_PATH" ]; then
        $FLINK_HOME/bin/flink savepoint $JOB_ID $SAVEPOINT_PATH
    else
        $FLINK_HOME/bin/flink savepoint $JOB_ID
    fi
    
    if [ $? -eq 0 ]; then
        log_info "保存点创建成功"
    else
        log_error "保存点创建失败"
        exit 1
    fi
}

# 主函数
main() {
    echo "========================================="
    echo "       Flink Template 部署工具         "
    echo "========================================="
    
    # 初始化变量
    COMMAND="submit"
    FLINK_HOME="$DEFAULT_FLINK_HOME"
    JAR_PATH="$DEFAULT_JAR_PATH"
    JOB_NAME="$DEFAULT_JOB_NAME"
    PARALLELISM="$DEFAULT_PARALLELISM"
    CONFIG_FILE="$DEFAULT_CONFIG_FILE"
    ENVIRONMENT="dev"
    JOB_ID=""
    SAVEPOINT_PATH=""
    
    # 解析命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            submit|cancel|list|status|savepoint|stop)
                COMMAND="$1"
                shift
                ;;
            --flink-home)
                FLINK_HOME="$2"
                shift 2
                ;;
            --jar-path)
                JAR_PATH="$2"
                shift 2
                ;;
            --job-name)
                JOB_NAME="$2"
                shift 2
                ;;
            --parallelism)
                PARALLELISM="$2"
                shift 2
                ;;
            --config-file)
                CONFIG_FILE="$2"
                shift 2
                ;;
            --job-id)
                JOB_ID="$2"
                shift 2
                ;;
            --savepoint-path)
                SAVEPOINT_PATH="$2"
                shift 2
                ;;
            --env)
                ENVIRONMENT="$2"
                shift 2
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # 检查环境
    check_flink
    
    # 根据命令执行相应操作
    case $COMMAND in
        submit)
            check_jar
            submit_job
            ;;
        cancel)
            cancel_job
            ;;
        list)
            list_jobs
            ;;
        status)
            show_status
            ;;
        savepoint)
            create_savepoint
            ;;
        stop)
            stop_job
            ;;
        *)
            log_error "未知命令: $COMMAND"
            show_help
            exit 1
            ;;
    esac
}

# 脚本错误时的清理
trap 'log_error "部署过程中发生错误，请检查日志"; exit 1' ERR

# 执行主函数
main "$@" 