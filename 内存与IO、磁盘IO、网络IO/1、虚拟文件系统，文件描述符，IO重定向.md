# 虚拟文件系统 - VFS 

每个程序都是线性的、逻辑的虚拟地址，通过MRU转到CPU物理地址，一个程序想要获取IO中数据时，一般通过内核调用IO数据，当然程序也可以直接获取IO数据

内核包含哪些内容？

- VFS 目录树结构。树上不同节点可以映射到不同的物理位置，每个物理位置可以是不同的、具体的文件系统。比如网络上的节点。windows属于物理文件系统。VFS可以想象成暴露给用户空间的a统一接口，实现以及驱动可以不一样。每个程序都是FD，通过seek指针，也就是偏移量找到自己该读写的pagecache，修改同一数据会加锁
  - inode，可以抽象成id。每个文件都有一个inode号，inode号在内存里
  - pagecache，页缓存，默认4K。
    - 一个程序想要读数据，通常默认的是第一步访问内核，告诉内核需要加载哪个文件，inode号被加载，元数据被加载，先由内核去读，读到内存里面，开辟一个pagecache。
    - 两个程序想打开同一个文件，pagecache会让共享出来
    - dirty，程序读取了磁盘里文件到pagecache，然后程序改了某个pagecache中的数据，修改过的pagecache被标记为dirty
    - flush，将标记为dirty的pagecache刷新到磁盘中。可以交由内核在特定时间刷新到磁盘，也可以在修改完后立即让内核刷新到磁盘

**实际操作**

*查看虚拟目录树结构*

```shell
# Linux有3个分区，sda1 BIOS时加载、swap 交换分区、sda3 root分区
cd /
df #linux中df命令的功能是用来检查linux服务器的文件系统的磁盘空间占用情况。可以利用该命令来获取硬盘被占用了多少空间，目前还剩下多少空间等信息。boot目录是属于sda3，但是sda1覆盖了sda3
df -h #方便阅读方式显示
cd /boot/ #包含内核的镜像
cd /
umount /boot #卸载boot，删除sda1分区的boot目录，还有boot目录，此时的目录是sda3分区的
mount /dev/sdal /boot #将sda1挂载到root目录
# 卸载、挂载对程序没有影响。因为目录树的结构趋向于稳定，有一个映射的过程
```

# 文件描述符和IO重定向

> 首先得知道冯诺依曼体系
>
> 计算器、控制器 - CPU
>
> 主存储器 - 内存
>
> 输入输出设备 - I/O
>
>  
>
> Linux将其抽象成 <span style='color:red'>一切皆文件</span>

```shell
ll # 显示文件信息第一列表示文件类型

- ：普通文件（可执行文件、图片、文本）
d ：目录
l ：链接
b ：块设备（硬盘）
c ：字符设备（键盘、网卡）
s ：socket
p ：pipeline 管道
[eventpoll] ：epoll提供的内存区域

## 演示 链接 l
# 硬链接
vi test.txt # 随便输入几个字符
ln test.txt test2.txt #两个文件引用数量都显示为2
# -rw-r--r--. 2 root root 6 Jan 13 10:22 test2.txt
# -rw-r--r--. 2 root root 6 Jan 13 10:22 test.txt
stat test.txt
stat test2.txt
# 两个文件inode号一样
# 两个变量名指向同一个物理文件，就是硬链接
# 修改一个文件，另一个文件也会被修改，软链接也是一样
rm -f test.txt # 会有影响吗？不会，只是删了引用，test2.txt引用降为1

# 软链接
ln -s test2.txt test.txt #引用数量为1，没有变成2
# -rw-r--r--. 1 root root 11 Jan 13 10:31 test2.txt
# lrwxrwxrwx. 1 root root  9 Jan 13 10:33 test.txt -> test2.txt
stat test.txt
stat test2.txt
# inode号不一样
rm -f test2.txt # test.txt指向就会报错

## 演示 块设备
# dd：创建块设备
# if：input file
# of：output file
# bs：block size
# count：bs数量
# 表示创建mydisk.img文件，大小为100M
dd  if=/dev/zero  of=mydisk.img  bs=1048576  count=100

# 使用 losetup将磁盘镜像文件虚拟成块设备
losetup /dev/loop0 mydisk.img

# 利用格式化工具，格式化成ext2文件格式，类似Windows格式化磁盘
mke2fs /dev/loop0

# 挂载块设备
mount -t ext2 /dev/loop0 /root/test/tmp

# bash路径
whereis bash

# copy bash到/root/test/tmp
mkdir bin
cp /usr/bin/bash bin/
cd bin/

# 分析动态链接库有哪些
ldd bash
	linux-vdso.so.1 =>  (0x00007ffd969ea000)
	libtinfo.so.5 => /lib64/libtinfo.so.5 (0x00007fbb94fa2000)
	libdl.so.2 => /lib64/libdl.so.2 (0x00007fbb94d9e000)
	libc.so.6 => /lib64/libc.so.6 (0x00007fbb949d0000)
	/lib64/ld-linux-x86-64.so.2 (0x00007fbb951cc000)

# lib64目录下文件copy到当前lib64目录下
mkdir lib64
cp /lib64/{libtinfo.so.5,libdl.so.2,libc.so.6,ld-linux-x86-64.so.2} ./lib64/

# 开启当前bin目录的bash
chroot ./
echo "hello world" >/abc.txt # 此文件会在当前目录下创建

# 卸载loop设备
umount /root/test/tmp

## 演示 fd
# 通过当前进程显示该进程打开的文件，lsof没有，yum install lsof -y 安装
lsof -p $$
# TYPE 中 CHR 表示字符设备，REG 普通文件
# 每个进程都有 0u 1u 2u 这3个fd，u表示读写都可以
# 0u、1u、2u分别表示标准输入、标准输出和标准错误输出； 3u表示处于LISTEN状态的监听socket；4u表示epoll内核事件表对应的文件描述符
exec 8< test.txt # 创建fd号为8的fd
lsof -p $$ # 显示 fd 为 8r，r表示只读，type为 REG
# offset 为 0，inode号为 33591789
stat /root/test.txt # inode号一样

# 读取 fd 为8的第一行
read a 0<& 8
# 显示读取数据，数据为qwertyuu
echo $a
# 此时offset变成9了，o表示offset
lsof -op $$

# 打开新的会话也就是新的程序，发现没有 fd 8，因为内核为每个程序维护一份数据，其中就包括fd

## 演示 socket
exec 8<> /dev/tcp/www.baidu.com/80
# lrwx------. 1 root root 64 Jan 13 12:45 8 -> socket:[25141]
lsof -op $$
#COMMAND  PID  USER   FD   TYPE DEVICE OFFSET NODE NAME
# bash 	  1832 root   8u   IPv4 25141  0t0    TCP  linux01:49480->182.61.200.6:http (ESTABLISHED)

## 演示 IO重定向 与 pipeline
/proc # 内核对进程进行管理的文件
/proc/$$ # $$ 当前bash的pid

# IO重定向机制，可以让IO指向别的地方
# 重定向机制的实现，流的方向无非输入、输出，换成操作符 <  >
# 1 标准输出，输出到ls.out文件
ls ./ 1> /root/ls.out
# 标准输入来自于 test.txt，标准输出到 cat.out
cat 0< test.txt 1> cat.out
# read 遇到换行符停止读操作，a是变量，只会读取第一行数据
read a 0< cat.out
echo $a
# /qwer 不存在，肯定会报错，标准输出到 ls01.out，错误输出到 ls02.out
ls ./ /qwer 1> ls01.out 2> ls02.out
# 只有标准输出，没有报错，错误输出被覆盖了
ls ./ /qwer 1> ls03.out 2> ls03.out
# 操作描述符右边如果也是操作描述符，必须加上 &，但是此命令有问题，2指向1的位置，但是1指向其他位置，2就找不到文件
ls ./ /qwer 2>& 1  1> ls04.out
# 先让1 指向ls04.out，2再指向1 就不会报错了，ls04.out既有报错信息，又有标准输出
ls ./ /qwer  1> ls04.out 2>& 1

# 管道 | 
# head，tail取10行
head test.txt
tail test.txt
# 取2行，此时发现没有取中间一行的命令
head -2 test.txt
tail -2 test.txt
# head 取8行，通过管道交给 tail 取最后一行。这样就能取出第8行数据
head -8 test.txt | tail -1

# 父子进程隔离
echo $$ # 显示当前进程
a=1
echo $a # 显示为1
# 空格必须保留，输出 12345，交由cat 输出
{ a=9;  echo "12345"; } | cat
echo $a # 还是显示为1，管道 | 左右命令会各自开启子线程，通过管道合并输入输出，a=9在子进程执行，父子进程隔离级别很高，a的赋值不影响父进程的变量值
echo $$ | cat # 显示当前进程id，$$优先级高于管道，所有输出当前进程id
echo $BASHPID | cat # 显示子进程id

# 演示 pipeline
echo $$ # 显示当前线程id 2630
# 通过管道开启2个子进程
{ echo $BASHPID ; read x; } | { cat; echo $BASHPID ; read y; }
# 打开新的会话
ps -fe | grep 2630 # 显示8466 8467两个子进程
ll /proc/8466/fd # 1 -> pipe:[28086]
ll /proc/8467/fd # 0 -> pipe:[28086]
			  # FD	TYPE	DEVICE OFFSET NODE	NAME
lsof -op 8466 # 1w  FIFO    0,9    0t0    28086 pipe
lsof -op 8467 # 0r  FIFO    0,9    0t0    28086 pipe
```

