# JVM

## 1：JVM基础知识

1. 什么是JVM
2. 常见的JVM

## 2：ClassFileFormat

1. 递归函数java代码：

![递归函数](classFileFormat\Recursion.png)

2. main方法字节码：

![main方法字节码](classFileFormat\MainOfRecursion.png)

3. 递归函数方法字节码：

![递归方法字节码](classFileFormat\MethodOfRecursion.png)

4. 字节码栈帧解析：

![字节码栈帧解析](classFileFormat\StackFrame.png)

5. 解析字节码(recursion方法)
   1. num=3时，将num和1压入栈，为比较大小num和1都弹出栈，num不等于1，跳到第7步;两次将num压栈，再将1压栈，做sub操作，将最上面两个数num和1弹出栈做相减得到2;调用recursion方法从头开始，此时return的num=2
   2. num=2时跟操作1一样
   3. num=1时，发现与常量1相等，return的值为1;此时num=1时的栈帧，方法出口为num=2的栈帧;num=2的栈帧方法出口为num=3的栈帧;imul让几个数相乘，整个方法返回



## 3：类编译-加载-初始化

hashcode
锁的信息（2位 四种组合）
GC信息（年龄）
如果是数组，数组的长度

## 4：JMM

new Cat()
pointer -> Cat.class
寻找方法的信息

## 5：对象

1：句柄池 （指针池）间接指针，节省内存
2：直接指针，访问速度快

## 6：GC基础知识

栈上分配
TLAB（Thread Local Allocation Buffer）
Old
Eden
老不死 - > Old

## 7：GC常用垃圾回收器

new Object()
markword          8个字节
类型指针           8个字节
实例变量           0
补齐                  0		
16字节（压缩 非压缩）
Object o
8个字节 
JVM参数指定压缩或非压缩

