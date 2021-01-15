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
sysctl -a # 显示所有系统参数
    vm.dirty_background_bytes = 0
    vm.dirty_background_ratio = 10 # 缓存达到最大内存的百分之多少会后台异步写入到磁盘
    vm.dirty_bytes = 0
    vm.dirty_expire_centisecs = 3000 # 单位为 10ms，多长时间page 会过期
    vm.dirty_ratio = 30 # 缓存达到最大内存的百分之多少会阻塞其他线程，然后写入到磁盘
    vm.dirty_writeback_centisecs = 500 # 单位为 10ms，多长时间page 会写到磁盘

```



