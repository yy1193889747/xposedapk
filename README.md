# 蚂蚁森林自动收能量
-----
## 看了[四哥的分析](https://www.52pojie.cn/thread-794312-1-1.html)，和[好心网友](https://github.com/yongjun925/autocollectenergy)的源码。自己也尝试debug了一波，修改了部分参数，增加了消失能量的收取。
## 操作流程
* xposed框架安装（略）
* Build APK
* adb install -r debug.apk
## 修复记录
* 2019.1.28 修复BUG （int换为long）