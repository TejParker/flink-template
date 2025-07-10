#!/bin/bash

# Flink Template项目构建脚本
# 用途：编译、测试、打包项目

export JAVA_HOME=/opt/install/jdk-17.0.15

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

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

# 检查Java版本
check_java() {
    if ! command -v java &> /dev/null; then
        log_error "Java未安装，请先安装Java 11+"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1-2)
    if [[ $(echo "$JAVA_VERSION >= 11" | bc -l 2>/dev/null || echo "0") == "0" ]]; then
        log_error "Java版本需要11或以上，当前版本: $JAVA_VERSION"
        exit 1
    fi
    
    log_info "Java版本检查通过: $JAVA_VERSION"
}

# 检查Maven
check_maven() {
    if ! command -v mvn &> /dev/null; then
        log_error "Maven未安装，请先安装Maven"
        exit 1
    fi
    
    log_info "Maven检查通过: $(mvn -version | head -1)"
}

# 清理项目
clean_project() {
    log_info "清理项目..."
    mvn clean
}

# 编译项目
compile_project() {
    log_info "编译项目..."
    mvn compile
}

# 运行测试
run_tests() {
    log_info "运行测试..."
    mvn test
}

# 打包项目
package_project() {
    log_info "打包项目..."
    mvn package -DskipTests
    
    if [ -f "target/flink-template-1.0.0.jar" ]; then
        log_info "打包成功: target/flink-template-1.0.0.jar"
        
        # 显示jar包信息
        JAR_SIZE=$(du -h target/flink-template-1.0.0.jar | cut -f1)
        log_info "JAR包大小: $JAR_SIZE"
        
        # 创建发布目录
        RELEASE_DIR="release/$(date +%Y%m%d_%H%M%S)"
        mkdir -p "$RELEASE_DIR"
        
        # 复制jar包和配置文件
        cp target/flink-template-1.0.0.jar "$RELEASE_DIR/"
        cp src/main/resources/application.yml "$RELEASE_DIR/application.yml.example"
        cp scripts/*.sql "$RELEASE_DIR/" 2>/dev/null || true
        cp scripts/*.sh "$RELEASE_DIR/" 2>/dev/null || true
        
        log_info "发布包已创建: $RELEASE_DIR"
    else
        log_error "打包失败，未找到jar文件"
        exit 1
    fi
}

# 生成依赖报告
generate_dependency_report() {
    log_info "生成依赖报告..."
    mvn dependency:tree > target/dependency-tree.txt
    mvn dependency:analyze > target/dependency-analysis.txt
    log_info "依赖报告已生成: target/dependency-tree.txt, target/dependency-analysis.txt"
}

# 主函数
main() {
    echo "========================================="
    echo "       Flink Template 项目构建工具      "
    echo "========================================="
    
    # 检查环境
    check_java
    check_maven
    
    # 解析命令行参数
    SKIP_TESTS=false
    SKIP_PACKAGE=false
    CLEAN_ONLY=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-tests)
                SKIP_TESTS=true
                shift
                ;;
            --skip-package)
                SKIP_PACKAGE=true
                shift
                ;;
            --clean-only)
                CLEAN_ONLY=true
                shift
                ;;
            --help|-h)
                echo "用法: $0 [选项]"
                echo "选项:"
                echo "  --skip-tests     跳过测试"
                echo "  --skip-package   跳过打包"
                echo "  --clean-only     仅清理项目"
                echo "  --help, -h       显示帮助信息"
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                exit 1
                ;;
        esac
    done
    
    # 执行构建步骤
    clean_project
    
    if [ "$CLEAN_ONLY" = true ]; then
        log_info "清理完成"
        exit 0
    fi
    
    compile_project
    
    if [ "$SKIP_TESTS" = false ]; then
        run_tests
    else
        log_warn "跳过测试阶段"
    fi
    
    if [ "$SKIP_PACKAGE" = false ]; then
        package_project
        generate_dependency_report
    else
        log_warn "跳过打包阶段"
    fi
    
    log_info "构建完成!"
}

# 脚本错误时的清理
trap 'log_error "构建过程中发生错误，请检查日志"; exit 1' ERR

# 执行主函数
main "$@" 