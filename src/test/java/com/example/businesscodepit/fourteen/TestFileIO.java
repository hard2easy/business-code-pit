package com.example.businesscodepit.fourteen;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/12
 * 修改时间：
 *      1.文件读写需要确保字符编码一致
 *      2.类静态方法进行文件操作注意释放文件句柄
 *      3.注意读写文件要考虑设置缓冲区
 * 第一，
 *     如果需要读写字符流，那么需要确保文件中字符的字符集和字符流的字符集是一致的，
 *     否则可能产生乱码。
 * 第二，
 *      使用 Files 类的一些流式处理操作，注意使用 try-with-resources 包装 Stream，
 *      确保底层文件资源可以释放，避免产生 too many open files 的问题。
 * 第三，
 *      进行文件字节流操作的时候，一般情况下不考虑进行逐字节操作，
 *      使用缓冲区进行批量读写减少 IO 次数，性能会好很多。
 *      一般可以考虑直接使用缓冲输入输出流 BufferedXXXStream，
 *      追求极限性能的话可以考虑使用 FileChannel 进行流转发。
 * 最后我要强调的是，文件操作因为涉及操作系统和文件系统的实现，
 * JDK 并不能确保所有 IO API 在所有平台的逻辑一致性，代码迁移到新的操作系统或文件系统时，
 * 要重新进行功能测试和性能测试
 * @author yaoyong
 **/
public class TestFileIO {
    private Logger log = LoggerFactory.getLogger(TestFileIO.class);

    /**
     * FileReader读取乱码:
     *      FileReader 是以当前机器的默认字符集来读取文件的，FileReader 虽然方便但因为使用了默认字符集对环境产生了依赖
     *      如果希望指定字符集的话，需要直接使用 InputStreamReader 和 FileInputStream
     *
     * @throws IOException
     */
    @Test
    public void test1() throws IOException {
        //GBK 编码的汉字，每一个汉字两个字节
        Files.deleteIfExists(Paths.get("hello.txt"));
        Files.write(Paths.get("hello.txt"), "你好hi".getBytes(Charset.forName("GBK")));
        log.info("bytes:{}", Hex.encodeHexString(Files.readAllBytes(Paths.get("hello.txt"))).toUpperCase());

        char[] chars = new char[10];
        String content = "";
        try (FileReader fileReader = new FileReader("hello.txt")) {
            int count;
            while ((count = fileReader.read(chars)) != -1) {
                content += new String(chars, 0, count);
            }
        }
        //乱码
        log.info("result:{}", content);

        //UTF-8 编码的“你好”的十六进制是 E4BDA0E5A5BD，每一个汉字需要三个字节；而
        log.info("charset: {}", Charset.defaultCharset());
        Files.write(Paths.get("hello2.txt"), "你好hi".getBytes(Charsets.UTF_8));
        log.info("bytes:{}", Hex.encodeHexString(Files.readAllBytes(Paths.get("hello2.txt"))).toUpperCase());
    }

    @Test
    public void right1() throws IOException {
        char[] chars = new char[10];
        String content = "";
        try (FileInputStream fileInputStream = new FileInputStream("hello.txt");
             InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, Charset.forName("GBK"))) {
            int count;
            while ((count = inputStreamReader.read(chars)) != -1) {
                content += new String(chars, 0, count);
            }
        }
        log.info("result: {}", content);

        //该方法一样可以解决乱码(指定字符集读取) 但是会存在OOM的问题
        //因为该方法是把每一行的数据放在List<String>中,如果数据量过大就会OOM
        log.info("Files-result: {}", Files.readAllLines(Paths.get("hello.txt"), Charset.forName("GBK")).stream().findFirst().orElse(""));
    }

    /**
     * 与 readAllLines 方法返回 List<String> 不同，lines 方法返回的是 Stream<String>。
     * 这，使得我们在需要时可以不断读取、使用文件中的内容，
     * 而不是一次性地把所有内容都读取到内存中，因此避免了 OOM
     *
     * 需要注意的是需要使用try-with-resource的方式关闭lines
     * @throws IOException
     */
    @Test
    public void test2() throws IOException {
        //输出文件大小
        log.info("file size:{}", Files.size(Paths.get("test.txt")));
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("read 200000 lines");
        //使用Files.lines方法读取20万行数据
        log.info("lines {}", Files.lines(Paths.get("test.txt")).limit(200000).collect(Collectors.toList()).size());
        stopWatch.stop();
        stopWatch.start("read 2000000 lines");
        //使用Files.lines方法读取200万行数据
        log.info("lines {}", Files.lines(Paths.get("test.txt")).limit(2000000).collect(Collectors.toList()).size());
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());
        AtomicLong atomicLong = new AtomicLong();
        //使用Files.lines方法统计文件总行数
        Files.lines(Paths.get("test.txt")).forEach(line->atomicLong.incrementAndGet());
        log.info("total lines {}", atomicLong.get());

        //如果不使用try-with-resource的方式包裹,那么如果频繁操作文件会报错:Too many open files
        LongAdder longAdder = new LongAdder();
        IntStream.rangeClosed(1, 1000000).forEach(i -> {
            try (Stream<String> lines = Files.lines(Paths.get("demo.txt"))) {
                lines.forEach(line -> longAdder.increment());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        log.info("total : {}", longAdder.longValue());
    }

    /**
     * 我曾遇到过这么一个案例，一段先进行文件读入再简单处理后写入另一个文件的业务代码，
     * 由于开发人员使用了单字节的读取写入方式，导致执行得巨慢，业务量上来后需要数小时才能完成
     */
    @Test
    public void test3() throws IOException {
        //先生成一个文件用于测试
//        Files.write(Paths.get("src.txt"),
//                IntStream.rangeClosed(1, 1000000).mapToObj(i -> UUID.randomUUID().toString()).collect(Collectors.toList())
//                , Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        //这样的实现，复制一个 35MB 的文件居然耗时 190 秒 一个个字节读取写入。。。
        try (FileInputStream fileInputStream = new FileInputStream("src.txt");
             FileOutputStream fileOutputStream = new FileOutputStream("dest.txt")) {
            int i;
            while ((i = fileInputStream.read()) != -1) {
                fileOutputStream.write(i);
            }
        }
        // 一次读取100字节  性能远比上一操作好  并且缓存区根据实际情况进行修改
        // 在进行文件 IO 处理的时候，使用合适的缓冲区可以明显提高性能
        try (FileInputStream fileInputStream = new FileInputStream("src.txt");
             FileOutputStream fileOutputStream = new FileOutputStream("dest.txt")){
             byte[] buffer = new byte[100];
             int len = 0;
             while ((len = fileInputStream.read(buffer)) != -1) {
                 fileOutputStream.write(buffer, 0, len);
             }
        }
    }

    /**
     * 第一种方式虽然使用了缓冲流，但逐字节的操作因为方法调用次数实在太多还是慢，耗时 1.4 秒；
     * 后面两种方式的性能差不多，耗时 110 毫秒左右。
     * 虽然第三种方式没有使用缓冲流，但使用了 8KB 大小的缓冲区，和缓冲流默认的缓冲区大小相同
     * 既然这样使用 BufferedInputStream 和 BufferedOutputStream 有什么意义呢？
     *      为了演示所以示例三使用了固定大小的缓冲区，但在实际代码中每次需要读取的字节数很可能不是固定的，
     *      有的时候读取几个字节，有的时候读取几百字节，这个时候有一个固定大小较大的缓冲，
     *      也就是使用 BufferedInputStream 和 BufferedOutputStream 做为后备的稳定的二次缓冲，就非常有意义了
     * @throws IOException
     */
    @Test
    public void test31() throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("bufferedInputStream");
        bufferedStreamByteOperation();
        stopWatch.stop();
        stopWatch.start("bufferedInputStream-8-self");
        bufferedStreamBufferOperation();
        stopWatch.stop();
        stopWatch.start("FileInputStream-8");
        largerBufferOperation();
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());
    }
    //使用BufferedInputStream和BufferedOutputStream
    private static void bufferedStreamByteOperation() throws IOException {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream("src.txt"));
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream("dest.txt"))) {
            int i;
            while ((i = bufferedInputStream.read()) != -1) {
                bufferedOutputStream.write(i);
            }
        }
    }
    //额外使用一个8KB缓冲，再使用BufferedInputStream和BufferedOutputStream
    private static void bufferedStreamBufferOperation() throws IOException {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream("src.txt"));
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream("dest.txt"))) {
            byte[] buffer = new byte[8192];
            int len = 0;
            while ((len = bufferedInputStream.read(buffer)) != -1) {
                bufferedOutputStream.write(buffer, 0, len);
            }
        }
    }
    //直接使用FileInputStream和FileOutputStream，再使用一个8KB的缓冲
    private static void largerBufferOperation() throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream("src.txt");
             FileOutputStream fileOutputStream = new FileOutputStream("dest.txt")) {
            byte[] buffer = new byte[8192];
            int len = 0;
            while ((len = fileInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
            }
        }
    }

    /**
     * 对于类似的文件复制操作，如果希望有更高性能，可以使用 FileChannel 的 transfreTo 方法进行流的复制。
     * 在一些操作系统（比如高版本的 Linux 和 UNIX）上可以实现 DMA（直接内存访问），
     * 也就是数据从磁盘经过总线直接发送到目标文件，无需经过内存和 CPU 进行数据中转
     *
     *
     */
    @Test
    public void test4() throws IOException {
        FileChannel in = FileChannel.open(Paths.get("src.txt"), StandardOpenOption.READ);
        FileChannel out = FileChannel.open(Paths.get("dest.txt"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("transferTo");
        in.transferTo(0, in.size(), out);
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());
    }
}
