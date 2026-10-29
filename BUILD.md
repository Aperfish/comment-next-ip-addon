# 构建与验证

需要 JDK 21 或以上、Node.js、PowerShell 7 和原作者的 plugin-comment-next-1.0.13.jar。当前验证使用 JDK 25 编译器，应用代码目标 Java 21。请不要用之前的定制评论 JAR 作为构建输入。

仓库根目录执行：

```powershell
./tools/Get-Dependencies.ps1
./build.ps1 -OriginalJar 'D:/path/plugin-comment-next-1.0.13.jar'
```

已有依赖可显式提供：

```powershell
./build.ps1 -OriginalJar 'D:/path/plugin-comment-next-1.0.13.jar' -LibDirectory 'D:/path/lib'
```

所有依赖均按锁文件校验 SHA-256，不能随意换版本。网络下载失败时可将锁文件列出的 JAR 按 file 名保存到 .cache/dependencies。

输出为 dist/comment-next-ip-addon-1.0.13-4.jar。work/ 是自动生成目录，每次构建会校验绝对路径后清空重建；不要把输入 JAR、源码或其他个人文件放在里面。输入原版 JAR 不修改。

构建会从 src/main/java、src/main/resources 和 compile-fixtures 准备隔离的临时目录，再编译、重定位自有类与复用的网络类。原服务构造器参数继续引用原版真实类型，避免类名重定位造成 NoSuchMethodError。测试、签名夹具以及 Halo/Spring/PF4J 类不打入插件。

自动检查包括 31 项区县断言、19 项格式回归、依赖注入回归、真实原版服务类的 JVM 构造连接以及基础字节码检查。注入与连接检查使用编译签名夹具，不是真实 Halo Spring 容器验收。

JAR 的 ZIP 时间戳等构建元数据会导致逐次 SHA-256 变化，不承诺位级可复现。为每次实际发布的文件重新生成校验值。源码包与本资料包中的 release-assets JAR 对应，来源和验证记录见 docs/VERIFICATION.md。
