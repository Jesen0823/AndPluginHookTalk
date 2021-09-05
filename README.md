# AndPluginHookTalk
Hook  in Plugin Project

### hook按钮的点击监听

### 使用hook来启动一个没有在manifest注册的Activity

startActivity ---> AMS检测当前要启动的Activity是否已经在Manifest注册--->没注册就抛异常

目标： 通过hook把目标Activity换成一个已经被注册的Activity去检测，检测完成再启动原本的目标Activity



