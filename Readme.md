注意点：
签名时，主要增加`--min-sdk-version [你的最低反对版本]`，否则会报异常
```
apksigner sign --ks [.jks文件门路] --ks-key-alias [别名] --min-sdk-version [你的最低反对版本] --out [签名过导出的aab文件] [行将签名的aab]
```