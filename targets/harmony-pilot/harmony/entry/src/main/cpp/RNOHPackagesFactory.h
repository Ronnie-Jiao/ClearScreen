// 由 expo-harmony 生成 —— 受管文件，请勿手改（漂移保护见 docs/guide.md）。
#pragma once

// RNOH C++ 注册（autolinking）。与 RNOH 官方宿主脚手架兼容：
// 宿主 entry/src/main/cpp/PackageProvider.cpp 会调用 createRNOHPackages(ctx)；
// 内联实现按 content/autolink.json 注册表逐字展开（rnoh-package 条目 0 时为空集合）。
#include <memory>
#include <vector>
#include "RNOH/Package.h"

namespace rnoh {
inline std::vector<std::shared_ptr<rnoh::Package>> createRNOHPackages(
    rnoh::Package::Context ctx) {
  (void)ctx;
  std::vector<std::shared_ptr<rnoh::Package>> packages = {
  };
  return packages;
}
} // namespace rnoh

