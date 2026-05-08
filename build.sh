#!/usr/bin/env bash
set -euo pipefail

if [[ $# -eq 0 ]]; then
  echo "用法: $0 [--commit <COMMIT1,COMMIT2,...>] <CONFIG1> [CONFIG2 ...]" >&2
  exit 1
fi

use_commits=false
commit_ids=()

if [[ "${1:-}" == "--commit" ]]; then
  if [[ $# -lt 3 ]]; then
    echo "错误: 使用 --commit 时，必须提供 commit 列表和至少一个 config。" >&2
    echo "用法: $0 [--commit <COMMIT1,COMMIT2,...>] <CONFIG1> [CONFIG2 ...]" >&2
    exit 1
  fi

  use_commits=true
  IFS=',' read -r -a commit_ids <<< "$2"
  shift 2

  if [[ ${#commit_ids[@]} -eq 0 ]]; then
    echo "错误: commit 列表不能为空。" >&2
    exit 1
  fi
fi

configs=("$@")

if [[ ${#configs[@]} -eq 0 ]]; then
  echo "错误: 至少需要提供一个 config。" >&2
  echo "用法: $0 [--commit <COMMIT1,COMMIT2,...>] <CONFIG1> [CONFIG2 ...]" >&2
  exit 1
fi

if [[ "$use_commits" == true && ${#commit_ids[@]} -ne ${#configs[@]} ]]; then
  echo "错误: commit id 数量(${#commit_ids[@]})必须等于 config 数量(${#configs[@]})。" >&2
  exit 1
fi

if [[ "$use_commits" == true ]]; then
  # Validate all commits up-front so the build never starts with an invalid id.
  for commit_id in "${commit_ids[@]}"; do
    if ! git -C generators/riscv-boom rev-parse --verify --quiet "${commit_id}^{commit}" >/dev/null; then
      echo "错误: 无效的 riscv-boom commit id: ${commit_id}" >&2
      exit 1
    fi
  done
fi

for config in "${configs[@]}"; do
  if [[ -d "workspace/${config}" ]]; then
    echo "错误: 目录 workspace/${config} 已存在，请先清理后再构建。" >&2
    exit 1
  fi
done

for i in "${!configs[@]}"; do
  config="${configs[$i]}"
  echo "==> Building CONFIG=${config} BOARD=genesys2"

  # Case-insensitive check: switch rocket-chip branch based on "latency" in config
  config_lower=$(echo "$config" | tr '[:upper:]' '[:lower:]')

  if [[ "$config_lower" == *latency* ]]; then
    echo "    [rocket-chip] switching to 'latency' branch"
    git -C rocket-chip checkout latency
  else
    echo "    [rocket-chip] switching to 'default' branch"
    git -C rocket-chip checkout default
  fi

  if [[ "$use_commits" == true ]]; then
    target_commit="${commit_ids[$i]}"
    echo "    [riscv-boom] switching to commit '${target_commit}'"
    git -C generators/riscv-boom checkout --detach "${target_commit}"
  else
    # Case-insensitive check: switch riscv-boom branch based on keywords in config
    if [[ "$config_lower" == *coupled* ]]; then
      echo "    [riscv-boom] switching to 'true_coupled_baseline' branch"
      git -C generators/riscv-boom checkout true_coupled_baseline
    elif [[ "$config_lower" == *unmodified* ]]; then
      echo "    [riscv-boom] switching to 'unmodified' branch"
      git -C generators/riscv-boom checkout unmodified
    else
      echo "    [riscv-boom] switching to 'dev' branch"
      git -C generators/riscv-boom checkout dev
    fi
  fi

  make MAX_THREADS=12 "CONFIG=${config}" BOARD=genesys2 bitstream
done

echo "All builds completed successfully."
