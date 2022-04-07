
##### 工程目录说明
- demo 待加固工程
- jiagu_shell 壳工程
- jiaguLib，java工程，执行APK加固工作

运行build.gradle中的任务`jiagu`，可直接在工程下生成一个`jiagu`目录，进入该目录进行如下操作，

1.将需要加固的APK放于该目录中；

2.在`keystore.cfg`中按格式配置签名文件信息；

3.`cmd`进入到当前目录，然后运行如下命令开始加固

>  java -jar jiaguLib.jar [apk名称] keystore.cfg


`注：`
- 如果需要直接在工程中运行，请修改变量`isRelease`为false

- 加固过程使用到的命令：`dx`、`apktool`、`zipalign`、`apksigner`，若中间出现找不到对应命令，需要配置环境变量。

注意点：
签名时，主要增加`--min-sdk-version [你的最低反对版本]`，否则会报异常
```
apksigner sign --ks [.jks文件门路] --ks-key-alias [别名] --min-sdk-version [你的最低反对版本] --out [签名过导出的aab文件] [行将签名的aab]
```


关于原理详解可查看[APK加固原理详解](https://www.jianshu.com/p/89dee4891f70)

若想查看加密使用的C++工程请移步[jiagu_aes_project](https://github.com/zhang-hai/jiagu_aes_project)