package com.example.businesscodepit.seven;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/06
 * 修改时间：
 *    虽然数据保存在磁盘中，但其处理是在内存中进行的。为了减少磁盘随机读取次数，
 *    InnoDB 采用页而不是行的粒度来保存数据，即数据被分成若干页，以页为单位保存在磁盘中。
 *    InnoDB 的页大小，一般是 16KB
 *    页目录通过槽把记录分成不同的小组，每个小组有若干条记录。
 *    如图所示，记录中最前面的小方块中的数字，代表的是当前分组的记录条数，
 *    最小和最大的槽指向 2 个特殊的伪记录。有了槽之后，我们按照主键搜索页中记录时，
 *    就可以采用二分法快速搜索，无需从最小记录开始遍历整个页中的记录链表。
 *
 *    举一个例子，如果要搜索主键（PK）=15 的记录：
 *          先二分得出槽中间位是 (0+6)/2=3，看到其指向的记录是 12＜15，
 *          所以需要从 #3 槽后继续搜索记录；再使用二分搜索出 #3 槽和 #6 槽的中间位是 (3+6)/2=4.5 取整 4，
 *          #4 槽对应的记录是 16＞15，所以记录一定在 #3 槽中；
 *          再从 #3 槽指向的 12 号记录开始向下搜索 3 次，定位到 15 号记录。
 *          理解了 InnoDB 存储数据的原理后，我们就可以继续学习 MySQL 索引相关的原理和坑了。
 *
 *    页目录就是最简单的索引，是通过对记录进行一级分组来降低搜索的时间复杂度。
 *    但，这样能够降低的时间复杂度数量级，非常有限。
 *    当有无数个数据页来存储表数据的时候，我们就需要考虑如何建立合适的索引，才
 *    能方便定位记录所在的页,引入B+树
 *    B+ 树的特点
 *        最底层的节点叫作叶子节点，用来存放数据；
 *        其他上层节点叫作非叶子节点，仅用来存放目录项，作为索引；
 *        非叶子节点分为不同层次，通过分层来降低每一层的搜索量；
 *        所有节点按照索引键大小排序，构成一个双向链表，加速范围查找
 *
 *    聚簇索引与非聚簇索引
 *          聚簇索引具有唯一性,一张表只有一页,聚簇索引的叶子结点存储的是mysql数据页
 *          非聚簇索引一张表可以有多个,但是此时叶子结点存储的内容不是数据页而是数据主键id
 *          因此通过非聚簇索查询数据普遍需要进行回表,除非要查的数据都是非聚簇索引的组成部分
 *
 *    考虑额外创建二级索引的代价创建二级索引的代价，主要表现在维护代价、空间代价和回表代价三个方面。
 *
 *    页中的记录都是按照索引值从小到大的顺序存放的，新增记录就需要往页中插入数据，
 *       现有的页满了就需要新创建一个页，把现有页的部分数据移过去，这就是页分裂；
 *       如果删除了许多数据使得页比较空闲，还需要进行页合并。
 *       页分裂和合并，都会有 IO 代价，并且可能在操作过程中产生死锁
 *
 *    接下来，我就与你仔细分析下吧。
 *          维护代价：创建 N 个二级索引，就需要再创建 N 棵 B+ 树，
 *              新增数据时不仅要修改聚簇索引，还需要修改这 N 个二级索引
 *          空间代价：虽然二级索引不保存原始数据，但要保存索引列的数据，所以会占用更多的空间
 *              查询表索引耗费空间：SELECT DATA_LENGTH, INDEX_LENGTH FROM information_schema.TABLES WHERE TABLE_NAME='person'
 *          回表的代价：二级索引不保存原始数据，通过索引找到主键后需要再查询聚簇索引，才能得到我们要的数据
 *    索引最佳实践
 *          第一，无需一开始就建立索引，可以等到业务场景明确后，或者是数据量超过 1 万、查询变慢后，
 *              再针对需要查询、排序或分组的字段创建索引。
 *              创建索引后可以使用 EXPLAIN 命令，确认查询是否可以使用索引。
 *          第二，尽量索引轻量级的字段，比如能索引 int 字段就不要索引 varchar 字段。
 *              索引字段也可以是部分前缀，在创建的时候指定字段索引长度。针对长文本的搜索，可以考虑使用 Elasticsearch 等专门用于文本搜索的索引数据库。
 *          第三，尽量不要在 SQL 语句中 SELECT *，而是 SELECT 必要的字段，
 *              甚至可以考虑使用联合索引来包含我们要搜索的字段，既能实现索引加速，又可以避免回表的开销
 *
 *    不是所有针对索引列的查询都能用上索引
 *          索引只能匹配列前缀
 *          条件涉及函数操作无法走索引
 *          联合索引只能匹配左边的列
 *    数据库基于成本决定是否走索引
 *          查询数据可以直接在聚簇索引上进行全表扫描，也可以走二级索引扫描后到聚簇索引回表。
 *          看到这里，你不禁要问了，MySQL 到底是怎么确定走哪种方案的呢。
 *          MySQL 在查询数据之前，会先对可能的方案做执行计划，然后依据成本决定走哪个执行计划
 *              IO 成本，是从磁盘把数据加载到内存的成本。默认情况下，读取数据页的 IO 成本常数是 1（也就是读取 1 个页成本是 1）。
 *              CPU 成本，是检测数据是否满足条件和排序等 CPU 操作的成本。默认情况下，检测记录的成本是 0.2
 *          SHOW TABLE STATUS LIKE 'person'  查看表的行数以及数据长度
 *          MySQL 选择索引，并不是按照 WHERE 条件中列的顺序进行的；即便列有索引，
 *          甚至有多个可能的索引方案，MySQL 也可能不走索引
 *     使用optimizer_trace查看执行计划,可以看出各种方案的成本,以此了解MySQL的方案选择
 *          SET optimizer_trace="enabled=on";
 *          SELECT * FROM person WHERE NAME >'name84059' AND create_time>'2020-01-24 05:00:00';
 *          SELECT * FROM information_schema.OPTIMIZER_TRACE;
 *          SET optimizer_trace="enabled=off";
 * @author yaoyong
 **/
public class DataBaseIndex {
}
