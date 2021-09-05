# AndPluginHookTalk
Hook  in Plugin Project

### hook按钮的点击监听

### 使用hook来启动一个没有在manifest注册的Activity

startActivity ---> AMS检测当前要启动的Activity是否已经在Manifest注册--->没注册就抛异常`this activity in your AndroidManifest.xml?`

目标： 通过hook把目标Activity换成一个已经被注册的Activity去检测，检测完成再启动原本的目标Activity
      使得DstNoManifestActivity不在AndroidManifest里面注册，也能启动

具体过程：

1.替换（把系统里面的替换成动态代理)
2.添加动态代理（做替换工作)

1.把DstNoManifestActivity替换我们真实有效的ProxyActivity
startActivity(DstNoManifestActivity) --->  
     Activity --> Instrumentation.execStartActivity  
     ---> ActivityManagerNative.getDefaultIActivityManager.startActivity  
       --->(Hook) AMS.startActivity(检测，当前要启动的Activity是否注册了)

思想切入点:既然会得到IActivityManager，会设置IActivityManager，(寻找替换点(动态代理))

2.ASM检查过后，要把这个ProxyActivity换回来--> DstNoManifestActivity
startActivity -- DstNoManifestActivity -- (Hook ProxyActivity)（ANS）检测，当前要启动的Activity是否注册了) ok ---》>ActivityThread(即将加载启动Activity) ----(要把这个ProxyActivity 换回回DstNoManifestActivity)
Hook LAUNCH_ACTIVITY
要在Handler，handleMessage 之前执行



