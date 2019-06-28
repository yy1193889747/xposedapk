# 蚂蚁森林自动收能量
-----
#### 看了[四哥的分析](https://www.52pojie.cn/thread-794312-1-1.html)，和[热心网友](https://github.com/yongjun925/autocollectenergy)的源码。自己也尝试debug了一波，修改了部分参数，增加了好友消失能量的收取。

## 操作流程
* xposed框架安装（略）
* Build APK
* 手机连接电脑，打开开发者模式
* adb install -r app-debug.apk （见根目录）
## 修复记录
* 2019.1.28 修复BUG （Integer换为Long）
* 2019.6.26 修复BUG （包名更改） 10-1-65.apk
## 热心网友
![](\app\src\main\res\wx.png "咖啡")