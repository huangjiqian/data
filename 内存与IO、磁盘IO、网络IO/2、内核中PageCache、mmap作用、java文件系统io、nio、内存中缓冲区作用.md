# pagecache

pagecache是内核的折中方案

内核把磁盘文件读取到内存，使用页缓存，用户层面也有buffer IO缓存。

用户层与内核层通过 system call通信，系统调用读到 callback方法后保护线程，此时用户态切换到内核态

- system call 怎么实现的呢？通过 int 0x80。int 是 CPU 的指令；0x80，十进制128 - 1000 0000，这是一个值，放到寄存器中，与中断描述符作匹配的，中断描述符有0~255，128调用 callback 方法

用户调用某个程序，内核会读取fd，内核会从磁盘读取到pagecache，pagecache会优先在内核堆积。硬盘 copy到DMA，DMA copy到内核，内核再copy到用户空间，效率较快

- 没有协处理器 DMA，磁盘会copy到CPU，CPU copy到内核，内核copy到用户空间，效率很低

物理内存中，程序可能出现碎片化的情况，碎片之间不连续的，每个程序都有虚拟内存地址，线性的记录程序在物理内存中的碎片，每个碎片都是4K，也就是 page，使用时会调用需要的 page，不会全量分配。如果程序执行时没有需要的page，会有缺页异常，触发软中断，用户态切换到内核态，内核会在物理地址增加 page，程序指向page。

pagecache也是内存优化，没有页缓存，每次加载文件都需要从磁盘读取，大量消耗IO资源

pagecache 由内核维护，属于中间层，好处是数据访问更轻量，但是副作用是可靠性、一致性出现问题

```bash
sysctl -a | grep dirty # 显示所有系统参数
    vm.dirty_background_bytes = 0
    vm.dirty_background_ratio = 10 # 缓存达到最大内存的百分之多少会后台异步写入到磁盘
    vm.dirty_bytes = 0
    vm.dirty_expire_centisecs = 3000 # 单位为 10ms，多长时间page 会过期
    vm.dirty_ratio = 30 # 缓存达到最大内存的百分之多少会阻塞其他线程，然后写入到磁盘
    vm.dirty_writeback_centisecs = 500 # 单位为 10ms，多长时间page 会写到磁盘

```

# java文件系统IO

pagecache 需要用到工具 pcstat

```shell
pcstat /usr/bin/bash
# cached：缓存大小
# percent：缓存的百分比，100%说明都在缓存中
+---------------+----------------+------------+-----------+---------+
| Name          | Size (bytes)   | Pages      | Cached    | Percent |
|---------------+----------------+------------+-----------+---------|
| /usr/bin/bash | 964536         | 236        | 236       | 100.000 |
+---------------+----------------+------------+-----------+---------+
```

准备 java文件，定义几种方法，普通文件读写、buffer 文件读写、随机文件读写

```java
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class OSFileIO {

    static byte[] data = "123456789\n".getBytes();
    static String path = "/root/testfileio/out.txt";

    public static void main(String[] args) throws Exception{

        switch (args[0]) {
            case "0" :
                testBasicFileIO();
                break;
            case "1" :
                testBufferedFileIO();
                break;
            case "2" :
                testRandomAccessFileWrite();
                break;
            case "3":
                whatByteBuffer();
                break;
            default:
        }
    }

    /**
     * 测试最基本的file写
     * @throws Exception
     */
    public static void testBasicFileIO() throws Exception {
        File file = new File(path);
        FileOutputStream out = new FileOutputStream(file);
        while (true) {
            out.write(data);
        }
    }

    /**
     * 测试 buffer 文件 IO
     * jvm 提供 8KB 的空间，写满后调用 system call，写入到内核
     */
    public static void testBufferedFileIO() throws Exception {
        File file = new File(path);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        while (true) {
            bos.write(data);
        }
    }

    /**
     * 测试文件 NIO
     * @throws Exception
     */
    public static void testRandomAccessFileWrite() throws Exception {
        // 随机读写
        RandomAccessFile raf = new RandomAccessFile(path, "rw");
        //RandomAccessFile raf = new RandomAccessFile(, "rw");

        raf.write("hello my team\n".getBytes());
        raf.write("hello world\n".getBytes());
        System.out.println("write-----------------");
        System.in.read();

        // 可以修改指针偏移，而普通 file io不能修改指针偏移
        raf.seek(4);
        raf.write("qazx".getBytes());

        System.out.println("seek---------------");
        System.in.read();

        // NIO starting
        FileChannel rafChannel = raf.getChannel();
        // mmap - memory map 得到堆外内存 和 文件映射的   堆内没有对象的概念
        MappedByteBuffer map = rafChannel.map(FileChannel.MapMode.READ_WRITE, 0, 4096);

        /**
         * 不是系统调用，但是数据会到达内核的 pagecache
         * 曾经需要 out.write() 这样的系统调用，才能让程序的data 进入到内核的 pagecache
         * 曾经必须由用户态 内核态的切换
         * mmap 的内存映射，依然是内核的 pagecache 体系所约束的 ！！！
         * 总之还是会丢数据
         * 可以去 GitHub找一些 其他 C 程序员写的 JNI 扩展库，使用 Linux内核的 Direct IO
         * 直接 IO 是忽略Linux 的 pagecache
         * 是把 pagecache 交给了程序员自己开辟一个字节数组当中 pagecache，动用代码逻辑来维护一致性/dirty。。。一系列复杂问题
         * 唯一的好处可能是更细粒度的把控内核调用
         */
        map.put("@@@".getBytes());
        System.out.println("map-put---------------");
        System.in.read();

        //map.force(); // 类似 flush，强制刷新

        raf.seek(0);

        // 堆上的分配
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        // 堆外的分配
        //ByteBuffer buffer = ByteBuffer.allocateDirect(8192);

        int read = rafChannel.read(buffer); // 跟 buffer.put() 一样
        System.out.println("read of int :" + read);
        System.out.println(buffer);
        buffer.flip();
        System.out.println(buffer);

        for (int i=0; i < buffer.limit(); i++) {
            System.out.print(((char)buffer.get(i)));
        }
    }

    //@Test
    public static void whatByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        // 最重要的3个维度 position/limit/capacity，那么我以这3个维度判断调用的方法
        // position - 0
        // limit - 1024
        // capacity - 1024
        System.out.println("mark: " + buffer);

        // position - 3
        // limit - 1024
        // capacity - 1024
        buffer.put("123".getBytes());

        System.out.println("--------put:123..........");
        System.out.println("mark: " + buffer);

        // position - 0
        // limit - 3
        // capacity - 1024
        buffer.flip(); // 读写交替

        System.out.println("--------flip..........");
        System.out.println("mark: " + buffer);

        // position - 1
        // limit - 3
        // capacity - 1024
        buffer.get(); // 获取一个字节，position后移一位

        System.out.println("--------get..........");
        System.out.println("mark: " + buffer);

        // position - 2
        // limit - 1024
        // capacity - 1024
        buffer.compact(); // 压缩已读取的空间

        System.out.println("--------compact..........");
        System.out.println("mark: " + buffer);

        // position - 0
        // limit - 1024
        // capacity - 1024
        buffer.clear();

        System.out.println("--------clear..........");
        System.out.println("mark: " + buffer);
    }
}
```

```shell
mkdir /root/testfileio

# 将 java 文件放入
vi mysh
	rm -fr *out* # 删除out文件
	/usr/tool/jdk1.8/bin/javac OSFileIO.java # 编译 java文件
	# -ff：如果提供 -o filename ，那么所有进程的跟踪结果输出到相应的 filename.pid，pid为各进程的进程号
	strace -ff -o out /usr/tool/jdk1.8/bin/java OSFileIO $1 

chmod +x mysh # 改为可执行文件

ll -h && pcstat out.txt # 显示文件信息，并输出pagecache信息

# 为了测试，将 dirty 参数变大
vm.dirty_background_ratio = 90
vm.dirty_expire_centisecs = 30000
vm.dirty_ratio = 90
vm.dirty_writeback_centisecs = 5000

```

1、使用testBasicFileIO方法测试

测试pagecache 重启后数据是否存在

注意：vm中 "关机" 键相当于拔掉电源，内核不会flush 缓存的；而 "关闭客户机"会flush缓存。本次测试要点"关机" 键

```shell
# 执行普通文件
./mysh 0

# 关机前
ll -h && pcstat out.txt
    total 80M
    -rwxrwxrwx. 1 root root  110 Jan 16 00:25 mysh
    -rw-r--r--. 1 root root 3.8K Jan 16 00:43 OSFileIO.class
    -rw-r--r--. 1 root root 5.4K Jan 16 00:07 OSFileIO.java
    -rw-r--r--. 1 root root  15K Jan 16 00:43 out.84711
    -rw-r--r--. 1 root root  51M Jan 16 00:44 out.84712
    -rw-r--r--. 1 root root 8.2K Jan 16 00:44 out.84713
    -rw-r--r--. 1 root root  931 Jan 16 00:43 out.84714
    -rw-r--r--. 1 root root 1.1K Jan 16 00:43 out.84715
    -rw-r--r--. 1 root root  975 Jan 16 00:43 out.84716
    -rw-r--r--. 1 root root 8.2K Jan 16 00:43 out.84717
    -rw-r--r--. 1 root root 5.6K Jan 16 00:43 out.84718
    -rw-r--r--. 1 root root  931 Jan 16 00:43 out.84719
    -rw-r--r--. 1 root root 135K Jan 16 00:44 out.84720
    -rw-r--r--. 1 root root  12M Jan 16 00:44 out.txt
    +---------+----------------+------------+-----------+---------+
    | Name    | Size (bytes)   | Pages      | Cached    | Percent |
    |---------+----------------+------------+-----------+---------|
    | out.txt | 11666500       | 2849       | 2849      | 100.000 |
    +---------+----------------+------------+-----------+---------+

# 关机后
ll -h && pcstat out.txt
    total 16K
    -rwxrwxrwx. 1 root root  110 Jan 16 00:25 mysh
    -rw-r--r--. 1 root root 3.8K Jan 16 00:43 OSFileIO.class
    -rw-r--r--. 1 root root 5.4K Jan 16 00:07 OSFileIO.java
    -rw-r--r--. 1 root root    0 Jan 16 00:43 out.84711
    -rw-r--r--. 1 root root    0 Jan 16 00:43 out.84712
    -rw-r--r--. 1 root root    0 Jan 16 00:43 out.84713
    -rw-r--r--. 1 root root    0 Jan 16 00:43 out.84714
    -rw-r--r--. 1 root root    0 Jan 16 00:43 out.84715
    -rw-r--r--. 1 root root    0 Jan 16 00:43 out.84716
    -rw-r--r--. 1 root root    0 Jan 16 00:43 out.84717
    -rw-r--r--. 1 root root    0 Jan 16 00:43 out.84718
    -rw-r--r--. 1 root root    0 Jan 16 00:43 out.84719
    -rw-r--r--. 1 root root    0 Jan 16 00:43 out.84720
    -rw-r--r--. 1 root root    0 Jan 16 00:43 out.txt
    +---------+----------------+------------+-----------+---------+
    | Name    | Size (bytes)   | Pages      | Cached    | Percent |
    |---------+----------------+------------+-----------+---------|
    | out.txt | 0              | 0          | 0         |     NaN |
    +---------+----------------+------------+-----------+---------+
```

通过测试可以明显看出，当异常关机时，所有pagecache 数据都会丢失

```shell
# 查看主线程，一个byte 数组就会写到 pagecache 中
write(4, "123456789\n", 10)             = 10
```

2、使用testBufferedFileIO方法测试

测试 pagecache的淘汰机制

```shell
# 执行缓存文件IO
./mysh 1

# percent不是100%，说明pagecache超过阈值，使用LRU算法将数据从内存中淘汰，刷新到磁盘
ll -h && pcstat out.txt
    total 2.1G
    -rwxrwxrwx. 1 root root  110 Jan 16 00:25 mysh
    -rw-r--r--. 1 root root 3.8K Jan 16 01:06 OSFileIO.class
    -rw-r--r--. 1 root root 5.4K Jan 16 00:07 OSFileIO.java
    -rw-r--r--. 1 root root  15K Jan 16 01:06 out.1832
    -rw-r--r--. 1 root root 9.3M Jan 16 01:06 out.1833
    -rw-r--r--. 1 root root 4.9K Jan 16 01:06 out.1834
    -rw-r--r--. 1 root root  930 Jan 16 01:06 out.1835
    -rw-r--r--. 1 root root 1.1K Jan 16 01:06 out.1836
    -rw-r--r--. 1 root root  974 Jan 16 01:06 out.1837
    -rw-r--r--. 1 root root 9.1K Jan 16 01:06 out.1838
    -rw-r--r--. 1 root root 6.6K Jan 16 01:06 out.1839
    -rw-r--r--. 1 root root  930 Jan 16 01:06 out.1840
    -rw-r--r--. 1 root root  52K Jan 16 01:06 out.1841
    -rw-r--r--. 1 root root 1.2G Jan 16 01:06 out.txt
    +---------+----------------+------------+-----------+---------+
    | Name    | Size (bytes)   | Pages      | Cached    | Percent |
    |---------+----------------+------------+-----------+---------|
    | out.txt | 1240711290     | 302909     | 242035    | 079.904 |
    +---------+----------------+------------+-----------+---------+
```

此时将 out.txt 修改为 old.txt，使用 mv out.txt old.txt改名。再次开启 ./mysh 1

```shell
ll -h && pcstat old.txt && pcstat out.txt
    total 3.1G
    -rwxrwxrwx. 1 root root  110 Jan 16 00:25 mysh
    -rw-r--r--. 1 root root 1.2G Jan 16 01:19 old.txt
    -rw-r--r--. 1 root root 3.8K Jan 16 01:23 OSFileIO.class
    -rw-r--r--. 1 root root 5.4K Jan 16 00:07 OSFileIO.java
    -rw-r--r--. 1 root root  15K Jan 16 01:23 out.1960
    -rw-r--r--. 1 root root 9.9M Jan 16 01:23 out.1961
    -rw-r--r--. 1 root root 4.4K Jan 16 01:23 out.1962
    -rw-r--r--. 1 root root 1.1K Jan 16 01:23 out.1963
    -rw-r--r--. 1 root root 1.1K Jan 16 01:23 out.1964
    -rw-r--r--. 1 root root 2.2K Jan 16 01:23 out.1965
    -rw-r--r--. 1 root root 9.0K Jan 16 01:23 out.1966
    -rw-r--r--. 1 root root 5.9K Jan 16 01:23 out.1967
    -rw-r--r--. 1 root root  960 Jan 16 01:23 out.1968
    -rw-r--r--. 1 root root  39K Jan 16 01:23 out.1969
    -rw-r--r--. 1 root root 1.9K Jan 16 01:23 out.2012
    -rw-r--r--. 1 root root 1.3G Jan 16 01:23 out.txt
    +---------+----------------+------------+-----------+---------+
    | Name    | Size (bytes)   | Pages      | Cached    | Percent |
    |---------+----------------+------------+-----------+---------|
    | old.txt | 1255707180     | 306570     | 0         | 000.000 |
    +---------+----------------+------------+-----------+---------+
    +---------+----------------+------------+-----------+---------+
    | Name    | Size (bytes)   | Pages      | Cached    | Percent |
    |---------+----------------+------------+-----------+---------|
    | out.txt | 1300211640     | 317435     | 254198    | 080.079 |
    +---------+----------------+------------+-----------+---------+
```

测试完可以看出，旧文件缓存慢慢被淘汰然后刷新到磁盘，新文件占用缓存

```shell
# 查看主线程输出，达到 8KB 写到 pagecache 中
write(4, "123456789\n123456789\n123456789\n12"..., 8190) = 8190
```

pagecache 优化了IO性能，减少IO此时。但是会丢失数据，pagecache 阈值越高，丢失数据可能越多

# NIO

测试方法testRandomAccessFileWrite，使用FileChannel调用 NIO

```shell
./mysh 2
    write-----------------

    seek---------------

    map-put---------------

    read of int :4096
    java.nio.HeapByteBuffer[pos=4096 lim=8192 cap=8192]
    java.nio.HeapByteBuffer[pos=0 lim=4096 cap=8192]
    @@@lqazx team
    hello world
```

MappedByteBuffer 使用到 mmap。mmap是什么呢？

​	mmap 全称 memory map，内存映射。简单来说就是直接访问 pagecache，跟在Linux直接读写数据一样。

​	当 mmap 修改 pagecache后，内核也会将 dirty 状态的 pagecache 刷新到磁盘

![mmap read与read system call区别](images\mmap read与read system call区别.webp)

上述代码中，很重要的一点：堆外分配与堆内分配。

```java
// 堆内的分配，产生的内存开销是在jvm之中
ByteBuffer buffer = ByteBuffer.allocate(8192);
// 堆外的分配，产生的内存开销是在jvm之外，也就是系统级别的内存分配
//ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
```

需要注意的是：allocateDirect 性能比 allocate 稍高一些，但是不是适用所有场景。适合在缓冲区生命周期长且能够重复使用的场景

NIO 会在堆内或堆外分配缓冲区，所有读写都会在缓冲区中使用系统调用 read/write 方法从内核缓冲区中读写

# 总结

mmap 与 NIO 知识点较多，后续会深入理解

操作系统没有绝对的数据可靠性。那么为啥还要设计 pagecache 呢？可以减少硬件 IO 调用次数，优先使用内存可以提高读写效率

即使按照想要的可靠性，将内核设置成直接刷新到磁盘的方式，不经过缓冲区，但是单点问题还是会让数据损耗，比如存储数据的硬盘损坏

所以要使用主从复制，主备高可用等方式，让数据更加可靠