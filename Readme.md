
##### 工程目录说明
- demo 待加固工程
- jiagu_shell 壳工程
- jiaguLib，java工程，执行APK加固工作

运行build.gradle中的任务`jiagu`，可直接在工程下生成一个`jiagu`目录，该目录加固需要的资源，将需要加固的APK放于该目录,按下操作进行加固：

1.在`keystore.cfg`中按格式配置签名文件信息；

2.运行如下命令开始加固；

>  java -jar jiaguLib.jar [apk名称] keystore.cfg


`注：`如果需要直接在工程中运行，请修改变量`isRelease`为false

注意点：
签名时，主要增加`--min-sdk-version [你的最低反对版本]`，否则会报异常
```
apksigner sign --ks [.jks文件门路] --ks-key-alias [别名] --min-sdk-version [你的最低反对版本] --out [签名过导出的aab文件] [行将签名的aab]
```


关于原理详解可查看[APK加固原理详解](https://www.jianshu.com/p/89dee4891f70)