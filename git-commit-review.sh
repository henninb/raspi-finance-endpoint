#!/bin/bash

# Git Commit Quality Reviewer for raspi-finance-endpoint
# Enforces Git best practices and project-specific requirements

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project-specific settings
PROJECT_NAME="raspi-finance-endpoint"
DEFAULT_BRANCH="main"
TEST_COMMAND="./gradlew clean build test integrationTest functionalTest"
LINT_COMMAND="./gradlew clean build -x test"

echo -e "${BLUE}🔍 Git Commit Quality Reviewer for ${PROJECT_NAME}${NC}"
echo "=================================================="

# Function to print colored messages
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check if we're in a git repository
check_git_repo() {
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "Not in a Git repository!"
        exit 1
    fi
    print_status "Git repository detected"
}

# Check current branch and suggest appropriate target
analyze_branch_strategy() {
    local current_branch=$(git rev-parse --abbrev-ref HEAD)
    local has_changes=$(git status --porcelain)

    print_info "Current branch: $current_branch"

    if [[ "$current_branch" == "$DEFAULT_BRANCH" ]]; then
        if [[ -n "$has_changes" ]]; then
            print_warning "Working on main branch with changes detected"
            echo "Consider creating a feature branch for:"
            echo "  - New features or significant changes"
            echo "  - Experimental code that needs testing"
            echo "  - Breaking changes"
            echo ""
            read -p "Continue on main branch? (y/N): " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                print_info "Consider: git checkout -b feature/your-feature-name"
                exit 0
            fi
        fi
        print_status "Committing to main branch"
        return 0
    else
        print_status "Working on feature branch: $current_branch"
        print_info "Good practice! Feature branches help with:"
        print_info "  - Isolated development"
        print_info "  - Code review process"
        print_info "  - Testing before merge"
        return 1
    fi
}

# Check and auto-fix trailing whitespace
check_trailing_whitespace() {
    print_info "Checking for trailing whitespace..."

    local files_with_whitespace=$(git diff --cached --check 2>/dev/null | grep "trailing whitespace" | cut -d: -f1 | sort -u)

    if [[ -n "$files_with_whitespace" ]]; then
        print_warning "Trailing whitespace found in staged files:"
        echo "$files_with_whitespace" | sed 's/^/  • /'
        echo ""

        read -p "Auto-fix trailing whitespace? (Y/n): " -n 1 -r
        echo

        if [[ $REPLY =~ ^[Nn]$ ]]; then
            print_error "Trailing whitespace must be fixed before commit (CLAUDE.md requirement)"
            print_info "Manual fix: sed -i 's/[[:space:]]*\$//' <filename>"
            return 1
        fi

        print_info "Auto-fixing trailing whitespace..."
        local fixed_count=0

        # Fix each file
        while IFS= read -r file; do
            if [[ -f "$file" ]]; then
                # Create backup
                cp "$file" "$file.backup"

                # Remove trailing whitespace
                sed -i 's/[[:space:]]*$//' "$file"

                # Check if file was actually changed
                if ! diff -q "$file" "$file.backup" > /dev/null 2>&1; then
                    print_status "Fixed: $file"
                    fixed_count=$((fixed_count + 1))

                    # Re-stage the fixed file
                    git add "$file"
                else
                    print_info "No changes needed: $file"
                fi

                # Clean up backup
                rm "$file.backup"
            fi
        done <<< "$files_with_whitespace"

        if [[ $fixed_count -gt 0 ]]; then
            print_status "Auto-fixed trailing whitespace in $fixed_count file(s)"
            print_info "Fixed files have been re-staged automatically"
        fi

        # Verify fix worked
        local remaining_whitespace=$(git diff --cached --check 2>/dev/null | grep "trailing whitespace" | cut -d: -f1 | sort -u)
        if [[ -n "$remaining_whitespace" ]]; then
            print_error "Some trailing whitespace could not be auto-fixed:"
            echo "$remaining_whitespace"
            return 1
        fi
    fi

    print_status "No trailing whitespace detected"
    return 0
}

# Analyze staged changes
analyze_staged_changes() {
    local staged_files=$(git diff --cached --name-only)

    if [[ -z "$staged_files" ]]; then
        print_error "No staged changes found!"
        print_info "Stage files with: git add <files>"
        return 1
    fi

    print_status "Staged files:"
    echo "$staged_files" | sed 's/^/  • /'

    # Check file types and suggest appropriate commit type
    local has_kotlin=$(echo "$staged_files" | grep -E "\.(kt|kts)$" || true)
    local has_groovy=$(echo "$staged_files" | grep -E "\.(groovy|gradle)$" || true)
    local has_sql=$(echo "$staged_files" | grep -E "\.(sql)$" || true)
    local has_config=$(echo "$staged_files" | grep -E "\.(yml|yaml|properties|json)$" || true)
    local has_docs=$(echo "$staged_files" | grep -E "\.(md|txt|adoc)$" || true)
    local has_tests=$(echo "$staged_files" | grep -E "(test|spec)" || true)

    print_info "Change analysis:"
    [[ -n "$has_kotlin" ]] && echo "  • Kotlin source files detected"
    [[ -n "$has_groovy" ]] && echo "  • Groovy/Gradle files detected"
    [[ -n "$has_sql" ]] && echo "  • SQL migration files detected"
    [[ -n "$has_config" ]] && echo "  • Configuration files detected"
    [[ -n "$has_docs" ]] && echo "  • Documentation files detected"
    [[ -n "$has_tests" ]] && echo "  • Test files detected"

    return 0
}

# Run build and tests
run_quality_checks() {
    print_info "Running quality checks..."

    # Check if gradlew exists
    if [[ ! -f "./gradlew" ]]; then
        print_warning "Gradle wrapper not found, skipping build checks"
        return 0
    fi

    print_info "Running lint/build check: $LINT_COMMAND"
    if ! $LINT_COMMAND > /tmp/build.log 2>&1; then
        print_error "Build/lint check failed!"
        echo "Last 20 lines of build output:"
        tail -20 /tmp/build.log
        return 1
    fi
    print_status "Build/lint check passed"

    # Ask about running full tests
    read -p "Run full test suite? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_info "Running full test suite: $TEST_COMMAND"
        if ! $TEST_COMMAND > /tmp/test.log 2>&1; then
            print_error "Tests failed!"
            echo "Last 30 lines of test output:"
            tail -30 /tmp/test.log
            return 1
        fi
        print_status "All tests passed"
    else
        print_warning "Skipping full test suite - remember to run before pushing!"
    fi

    return 0
}

# Generate commit message suggestions
suggest_commit_message() {
    local staged_files=$(git diff --cached --name-only)
    local diff_summary=$(git diff --cached --stat)

    print_info "Commit message suggestions based on changes:"
    echo ""

    # Analyze the changes to suggest message type
    if echo "$staged_files" | grep -q -E "(test|spec)"; then
        echo "📝 Test-related changes detected:"
        echo "  • test: add integration tests for transaction processing"
        echo "  • test: fix AccountSpec assertion logic"
        echo "  • test: update functional tests for GraphQL endpoints"
    fi

    if echo "$staged_files" | grep -q -E "\.(kt|kts)$"; then
        echo "🔧 Kotlin source changes detected:"
        echo "  • feat: add transaction categorization service"
        echo "  • fix: resolve null pointer exception in AccountController"
        echo "  • refactor: extract common validation logic to utils"
    fi

    if echo "$staged_files" | grep -q -E "\.sql$"; then
        echo "🗄️  Database changes detected:"
        echo "  • migration: add indexes for transaction queries"
        echo "  • migration: create user_preferences table"
    fi

    if echo "$staged_files" | grep -q -E "\.(yml|yaml|properties)$"; then
        echo "⚙️  Configuration changes detected:"
        echo "  • config: update CORS settings for production"
        echo "  • config: add new database connection properties"
    fi

    echo ""
    echo "📋 Commit message format:"
    echo "  <type>: <description>"
    echo ""
    echo "  Types: feat, fix, docs, style, refactor, test, chore, migration"
    echo "  Description: Present tense, lowercase, no period"
    echo ""
    echo "  Examples from your recent commits:"
    git log --oneline -5 | sed 's/^/    /'
    echo ""
}

# Create the commit
create_commit() {
    print_info "Creating commit..."

    # Get commit message
    echo "Enter commit message (or press Enter to open editor):"
    read -r commit_message

    if [[ -z "$commit_message" ]]; then
        # Open editor for multi-line message
        git commit -v
    else
        # Validate message format
        if [[ ${#commit_message} -lt 10 ]]; then
            print_warning "Commit message seems short. Consider adding more detail."
        fi

        if [[ ${#commit_message} -gt 72 ]]; then
            print_warning "First line is over 72 characters. Consider shortening."
        fi

        # Create commit
        git commit -m "$commit_message"
    fi

    print_status "Commit created successfully!"

    # Show the commit
    echo ""
    print_info "Commit details:"
    git show --stat HEAD
}

# Push to remote
handle_push() {
    local current_branch=$(git rev-parse --abbrev-ref HEAD)

    echo ""
    read -p "Push to remote? (y/N): " -n 1 -r
    echo

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if [[ "$current_branch" == "$DEFAULT_BRANCH" ]]; then
            print_info "Pushing to main branch..."
            git push origin "$current_branch"
        else
            print_info "Pushing feature branch..."
            git push -u origin "$current_branch"
            echo ""
            print_info "Consider creating a Pull Request for code review"
            print_info "Merge to main after: review, tests pass, and approval"
        fi
        print_status "Push completed!"
    else
        print_info "Commit created locally. Push when ready with:"
        print_info "  git push origin $current_branch"
    fi
}

# Main execution flow
main() {
    check_git_repo
    echo ""

    analyze_branch_strategy
    is_main_branch=$?
    echo ""

    analyze_staged_changes || exit 1
    echo ""

    check_trailing_whitespace || exit 1
    echo ""

    run_quality_checks || exit 1
    echo ""

    suggest_commit_message

    create_commit

    handle_push

    echo ""
    print_status "Git commit process completed successfully! 🎉"

    if [[ $is_main_branch -eq 0 ]]; then
        echo ""
        print_info "Branch Strategy Recommendations:"
        print_info "✅ Direct main commits are OK for: hotfixes, minor updates, docs"
        print_info "🔀 Use feature branches for: new features, experiments, breaking changes"
    fi
}

# Run the main function
main "$@"