import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

/** This is the parallel version of WordCount.java by way of
 * WordCountParallelBad.java.  The key change is that we use a
 * concurrent hashmap to allow parallel accesses, rather than a
 * synchronized HashMap.  Notice the updateCount() function needs be
 * concerned with maintaining the count correctly.
 */

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WordCountParallel implements Runnable {
    private final String buffer;
    private final ConcurrentMap<String,Integer> counts;
    
    public WordCountParallel(String buffer,
                             ConcurrentMap<String,Integer> counts) {
        this.counts = counts;
        this.buffer = buffer;
    }
    
    private final static String DELIMS = " :;,.=><?!/~[]™&©@#$%^*°•»-+_|—½”“‘’–\'\"\\{}()\t\n";
    private final static boolean printAll = false;
    
    /**
     * Looks for the last delimiter in the string, and returns its
     * index.
     */
    private static int findDelim(String buf) {
        for (int i = buf.length() - 1; i>=0; i--) {
            for (int j = 0; j < DELIMS.length(); j++) {
                char d = DELIMS.charAt(j);
                if (d == buf.charAt(i)) return i;
            }
        }
        return 0;
    }
    
    /**
     * Reads in a chunk of the file into a string.
     */
    private static String readFileAsString(BufferedReader reader, int size)
    throws java.io.IOException
    {
        StringBuffer fileData = new StringBuffer(size);
        int numRead=0;
        
        while(size > 0) {
            int bufsz = 1024 > size ? size : 1024;
            char[] buf = new char[bufsz];
            numRead = reader.read(buf,0,bufsz);
            if (numRead == -1)
                break;
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            size -= numRead;
        }
        return fileData.toString();
    }
    
    /**
     * Updates the count for each number of words.  Uses optimistic
     * techniques to make sure count is updated properly.
     */
    private void updateCount(String q) {
        Integer oldVal, newVal;
        Integer cnt = counts.get(q);
        // first case: there was nothing in the table yet
        if (cnt == null) {
            // attempt to put 1 in the table.  If the old
            // value was null, then we are OK.  If not, then
            // some other thread put a value into the table
            // instead, so we fall through
            oldVal = counts.put(q, 1);
            if (oldVal == null) return;
        }
        // general case: there was something in the table
        // already, so we have increment that old value
        // and attempt to put the result in the table.
        // To make sure that we do this atomically,
        // we use concurrenthashmap's replace() method
        // that takes both the old and new value, and will
        // only replace the value if the old one currently
        // there is the same as the one passed in.
        // Cf. http://www.javamex.com/tutorials/synchronization_concurrency_8_hashmap2.shtml
        do {
            oldVal = counts.get(q);
            newVal = (oldVal == null) ? 1 : (oldVal + 1);
        } while (!counts.replace(q, oldVal, newVal));
    }
    
    /**
     * Main task : tokenizes the given buffer and counts words.
     */
    public void run() {
        StringTokenizer st = new StringTokenizer(buffer,DELIMS);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            //System.out.println("updating count for "+token);
            updateCount(token);
        }
    }
    
    public static void main(String args[]) throws java.io.IOException {
        Set<String> checkSet = new HashSet<String>();
        for (int i = 0; i < 26; i++) {
            checkSet.add(String.valueOf((char)(i+97)));
        }
        for (int i = 0; i < 10; i++) {
            checkSet.add(new String() + i);
        }
        File inPutFile = new File("stopWords.txt");
        Scanner input = new Scanner(inPutFile);
        Set<String> stopWords = new HashSet<String>();
        while (input.hasNext()) {
            stopWords.add(input.next().trim());
        }
        input.close();
        long startTime = System.currentTimeMillis();
        File dir = new File("input_data"); // file name changed here
        ConcurrentMap<String,Integer> map = new ConcurrentHashMap<String,Integer>();
        for (File file: dir.listFiles()) {
            int numThreads = 4;
            int chunksize = 1000;
            //if (args.length >= 2)
            //numThreads = Integer.valueOf(args[1]);
            // if (args.length >= 3)
            // chunksize = Integer.valueOf(args[2]);
            ExecutorService pool = Executors.newFixedThreadPool(numThreads);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String leftover = ""; // in case a string broken in half
            while (true) {
                String res = readFileAsString(reader,chunksize);
                if (res.equals("")) {
                    if (!leftover.equals(""))
                        new WordCountParallel(leftover,map).run();
                    break;
                }
                int idx = findDelim(res);
                String taskstr = leftover + res.substring(0,idx);
                leftover = res.substring(idx,res.length());
                pool.submit(new WordCountParallel(taskstr,map));
            }
            pool.shutdown();
            try {
                pool.awaitTermination(1,TimeUnit.DAYS);
            } catch (InterruptedException e) {
                System.out.println("Pool interrupted!");
                System.exit(1);
            }
        }
        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        int total = 0;
        Set<Map.Entry<String,Integer>> entryset = map.entrySet();
        Map<String,Integer> alllowercase = new TreeMap<String,Integer>();
        Set<Map.Entry<String,Integer>> alllowercaseEntrySet = alllowercase.entrySet();
        boolean flag = false;
        for (Map.Entry<String,Integer> entry : entryset) {
            int count = entry.getValue();
            for (int i = 0; i < entry.getKey().length(); i++) {
                if (checkSet.contains(String.valueOf(entry.getKey().toLowerCase().charAt(i)))) {
                    flag = true;
                    String tmp = entry.getKey().toLowerCase().substring(i);
                    if(!stopWords.contains(tmp)){
                        if (alllowercase.containsKey(tmp)) {
                            for (Map.Entry<String,Integer> e: alllowercaseEntrySet) {
                                if (e.getKey().equals(tmp)) {
                                    int val = e.getValue() + count;
                                    e.setValue(val);
                                    break;
                                    
                                }
                            }
                        } else {
                            alllowercase.put(tmp, count);
                        }
                        //int count = entry.getValue();
                        if (printAll)
                            System.out.format("%-30s %d\n",entry.getKey(),count);
                        total += count;
                        break;
                    }else{
                        continue;
                    }
                }
            }
            if (!flag) {
                alllowercase.put(entry.getKey(), count);
            }
        }
        Map<String,Integer> a = new TreeMap<String,Integer>();
        Map<String,Integer> b = new TreeMap<String,Integer>();
        Map<String,Integer> c = new TreeMap<String,Integer>();
        Map<String,Integer> d = new TreeMap<String,Integer>();
        Map<String,Integer> e = new TreeMap<String,Integer>();
        Map<String,Integer> f = new TreeMap<String,Integer>();
        Map<String,Integer> g = new TreeMap<String,Integer>();
        Map<String,Integer> h = new TreeMap<String,Integer>();
        Map<String,Integer> i = new TreeMap<String,Integer>();
        Map<String,Integer> j = new TreeMap<String,Integer>();
        Map<String,Integer> k = new TreeMap<String,Integer>();
        Map<String,Integer> l = new TreeMap<String,Integer>();
        Map<String,Integer> m = new TreeMap<String,Integer>();
        Map<String,Integer> n = new TreeMap<String,Integer>();
        Map<String,Integer> o = new TreeMap<String,Integer>();
        Map<String,Integer> p = new TreeMap<String,Integer>();
        Map<String,Integer> q = new TreeMap<String,Integer>();
        Map<String,Integer> r = new TreeMap<String,Integer>();
        Map<String,Integer> s = new TreeMap<String,Integer>();
        Map<String,Integer> t = new TreeMap<String,Integer>();
        Map<String,Integer> u = new TreeMap<String,Integer>();
        Map<String,Integer> v = new TreeMap<String,Integer>();
        Map<String,Integer> w = new TreeMap<String,Integer>();
        Map<String,Integer> x = new TreeMap<String,Integer>();
        Map<String,Integer> y = new TreeMap<String,Integer>();
        Map<String,Integer> z = new TreeMap<String,Integer>();
        Map<String,Integer> m0 = new TreeMap<String,Integer>();
        Map<String,Integer> m1 = new TreeMap<String,Integer>();
        Map<String,Integer> m2 = new TreeMap<String,Integer>();
        Map<String,Integer> m3 = new TreeMap<String,Integer>();
        Map<String,Integer> m4 = new TreeMap<String,Integer>();
        Map<String,Integer> m5 = new TreeMap<String,Integer>();
        Map<String,Integer> m6 = new TreeMap<String,Integer>();
        Map<String,Integer> m7 = new TreeMap<String,Integer>();
        Map<String,Integer> m8 = new TreeMap<String,Integer>();
        Map<String,Integer> m9 = new TreeMap<String,Integer>();
        Map<String,Integer> fk = new TreeMap<String,Integer>();
        int compare = 0;
        for (Map.Entry<String,Integer> entry : alllowercaseEntrySet) {
            if (entry.getKey().toLowerCase().charAt(0) == 'a') {
                compare += entry.getValue();
                a.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'b') {
                compare += entry.getValue();
                b.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'c') {
                compare += entry.getValue();
                c.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'd') {
                compare += entry.getValue();
                d.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'e') {
                compare += entry.getValue();
                e.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'f') {
                compare += entry.getValue();
                f.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'g') {
                compare += entry.getValue();
                g.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'h') {
                compare += entry.getValue();
                h.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'i') {
                compare += entry.getValue();
                i.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'j') {
                compare += entry.getValue();
                j.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'k') {
                compare += entry.getValue();
                k.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'l') {
                compare += entry.getValue();
                l.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'm') {
                compare += entry.getValue();
                m.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'n') {
                compare += entry.getValue();
                n.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'o') {
                compare += entry.getValue();
                o.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'p') {
                compare += entry.getValue();
                p.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'q') {
                compare += entry.getValue();
                q.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'r') {
                compare += entry.getValue();
                r.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 's') {
                compare += entry.getValue();
                s.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 't') {
                compare += entry.getValue();
                t.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'u') {
                compare += entry.getValue();
                u.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'v') {
                compare += entry.getValue();
                v.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'w') {
                compare += entry.getValue();
                w.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'x') {
                compare += entry.getValue();
                x.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'y') {
                compare += entry.getValue();
                y.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().toLowerCase().charAt(0) == 'z') {
                compare += entry.getValue();
                z.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().charAt(0) == '0') {
                compare += entry.getValue();
                m0.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().charAt(0) == '1') {
                compare += entry.getValue();
                m1.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().charAt(0) == '2') {
                compare += entry.getValue();
                m2.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().charAt(0) == '3') {
                compare += entry.getValue();
                m3.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().charAt(0) == '4') {
                compare += entry.getValue();
                m4.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().charAt(0) == '5') {
                compare += entry.getValue();
                m5.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().charAt(0) == '6') {
                compare += entry.getValue();
                m6.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().charAt(0) == '7') {
                compare += entry.getValue();
                m7.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().charAt(0) == '8') {
                compare += entry.getValue();
                m8.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey().charAt(0) == '9') {
                compare += entry.getValue();
                m9.put(entry.getKey(), entry.getValue());
            } else
                fk.put(entry.getKey(), entry.getValue());
        }
        File outPutFilea = new File("out_data/a.txt");
        PrintWriter outputa = new PrintWriter(outPutFilea);
        for (Map.Entry<String,Integer> entry : a.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputa.println(entry.getKey() + " " + poss);
        }
        outputa.close();
        File outPutFileb = new File("out_data/b.txt");
        PrintWriter outputb = new PrintWriter(outPutFileb);
        for (Map.Entry<String,Integer> entry : b.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputb.println(entry.getKey() + " " + poss);
        }
        outputb.close();
        File outPutFilec = new File("out_data/c.txt");
        PrintWriter outputc = new PrintWriter(outPutFilec);
        for (Map.Entry<String,Integer> entry : c.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputc.println(entry.getKey() + " " + poss);
        }
        outputc.close();
        File outPutFiled = new File("out_data/d.txt");
        PrintWriter outputd = new PrintWriter(outPutFiled);
        for (Map.Entry<String,Integer> entry : d.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputd.println(entry.getKey() + " " + poss);
        }
        outputd.close();
        File outPutFilee = new File("out_data/e.txt");
        PrintWriter outpute = new PrintWriter(outPutFilee);
        for (Map.Entry<String,Integer> entry : e.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outpute.println(entry.getKey() + " " + poss);
        }
        outpute.close();
        File outPutFilef = new File("out_data/f.txt");
        PrintWriter outputf = new PrintWriter(outPutFilef);
        for (Map.Entry<String,Integer> entry : f.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputf.println(entry.getKey() + " " + poss);
        }
        outputf.close();
        File outPutFileg = new File("out_data/g.txt");
        PrintWriter outputg = new PrintWriter(outPutFileg);
        for (Map.Entry<String,Integer> entry : g.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputg.println(entry.getKey() + " " + poss);
        }
        outputg.close();
        File outPutFileh = new File("out_data/h.txt");
        PrintWriter outputh = new PrintWriter(outPutFileh);
        for (Map.Entry<String,Integer> entry : h.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputh.println(entry.getKey() + " " + poss);
        }
        outputh.close();
        File outPutFilei = new File("out_data/i.txt");
        PrintWriter outputi = new PrintWriter(outPutFilei);
        for (Map.Entry<String,Integer> entry : i.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputi.println(entry.getKey() + " " + poss);
        }
        outputi.close();
        File outPutFilej = new File("out_data/j.txt");
        PrintWriter outputj = new PrintWriter(outPutFilej);
        for (Map.Entry<String,Integer> entry : j.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputj.println(entry.getKey() + " " + poss);
        }
        outputj.close();
        File outPutFilek = new File("out_data/k.txt");
        PrintWriter outputk = new PrintWriter(outPutFilek);
        for (Map.Entry<String,Integer> entry : k.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputk.println(entry.getKey() + " " + poss);
        }
        outputk.close();
        File outPutFilel = new File("out_data/l.txt");
        PrintWriter outputl = new PrintWriter(outPutFilel);
        for (Map.Entry<String,Integer> entry : l.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputl.println(entry.getKey() + " " + poss);
        }
        outputl.close();
        File outPutFilem = new File("out_data/m.txt");
        PrintWriter outputm = new PrintWriter(outPutFilem);
        for (Map.Entry<String,Integer> entry : m.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputm.println(entry.getKey() + " " + poss);
        }
        outputm.close();
        File outPutFilen = new File("out_data/n.txt");
        PrintWriter outputn = new PrintWriter(outPutFilen);
        for (Map.Entry<String,Integer> entry : n.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputn.println(entry.getKey() + " " + poss);
        }
        outputn.close();
        File outPutFileo = new File("out_data/o.txt");
        PrintWriter outputo = new PrintWriter(outPutFileo);
        for (Map.Entry<String,Integer> entry : o.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputo.println(entry.getKey() + " " + poss);
        }
        outputo.close();
        File outPutFilep = new File("out_data/p.txt");
        PrintWriter outputp = new PrintWriter(outPutFilep);
        for (Map.Entry<String,Integer> entry : p.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputp.println(entry.getKey() + " " + poss);
        }
        outputp.close();
        File outPutFileq = new File("out_data/q.txt");
        PrintWriter outputq = new PrintWriter(outPutFileq);
        for (Map.Entry<String,Integer> entry : q.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputq.println(entry.getKey() + " " + poss);
        }
        outputq.close();
        File outPutFiler = new File("out_data/r.txt");
        PrintWriter outputr = new PrintWriter(outPutFiler);
        for (Map.Entry<String,Integer> entry : r.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputr.println(entry.getKey() + " " + poss);
        }
        outputr.close();
        File outPutFiles = new File("out_data/s.txt");
        PrintWriter outputs = new PrintWriter(outPutFiles);
        for (Map.Entry<String,Integer> entry : s.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputs.println(entry.getKey() + " " + poss);
        }
        outputs.close();
        File outPutFilet = new File("out_data/t.txt");
        PrintWriter outputt = new PrintWriter(outPutFilet);
        for (Map.Entry<String,Integer> entry : t.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputt.println(entry.getKey() + " " + poss);
        }
        outputt.close();
        File outPutFileu = new File("out_data/u.txt");
        PrintWriter outputu = new PrintWriter(outPutFileu);
        for (Map.Entry<String,Integer> entry : u.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputu.println(entry.getKey() + " " + poss);
        }
        outputu.close();
        File outPutFilev = new File("out_data/v.txt");
        PrintWriter outputv = new PrintWriter(outPutFilev);
        for (Map.Entry<String,Integer> entry : v.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputv.println(entry.getKey() + " " + poss);
        }
        outputv.close();
        File outPutFilew = new File("out_data/w.txt");
        PrintWriter outputw = new PrintWriter(outPutFilew);
        for (Map.Entry<String,Integer> entry : w.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputw.println(entry.getKey() + " " + poss);
        }
        outputw.close();
        File outPutFilex = new File("out_data/x.txt");
        PrintWriter outputx = new PrintWriter(outPutFilex);
        for (Map.Entry<String,Integer> entry : x.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputx.println(entry.getKey() + " " + poss);
        }
        outputx.close();
        File outPutFiley = new File("out_data/y.txt");
        PrintWriter outputy = new PrintWriter(outPutFiley);
        for (Map.Entry<String,Integer> entry : y.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputy.println(entry.getKey() + " " + poss);
        }
        outputy.close();
        File outPutFilez = new File("out_data/z.txt");
        PrintWriter outputz = new PrintWriter(outPutFilez);
        for (Map.Entry<String,Integer> entry : z.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputz.println(entry.getKey() + " " + poss);
        }
        outputz.close();
        File outPutFile0 = new File("out_data/0.txt");
        PrintWriter output0 = new PrintWriter(outPutFile0);
        for (Map.Entry<String,Integer> entry : m0.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            output0.println(entry.getKey() + " " + poss);
        }
        output0.close();
        File outPutFile1 = new File("out_data/1.txt");
        PrintWriter output1 = new PrintWriter(outPutFile1);
        for (Map.Entry<String,Integer> entry : m1.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            output1.println(entry.getKey() + " " + poss);
        }
        output1.close();
        File outPutFile2 = new File("out_data/2.txt");
        PrintWriter output2 = new PrintWriter(outPutFile2);
        for (Map.Entry<String,Integer> entry : m2.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            output2.println(entry.getKey() + " " + poss);
        }
        output2.close();
        File outPutFile3 = new File("out_data/3.txt");
        PrintWriter output3 = new PrintWriter(outPutFile3);
        for (Map.Entry<String,Integer> entry : m3.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            output3.println(entry.getKey() + " " + poss);
        }
        output3.close();
        File outPutFile4 = new File("out_data/4.txt");
        PrintWriter output4 = new PrintWriter(outPutFile4);
        for (Map.Entry<String,Integer> entry : m4.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            output4.println(entry.getKey() + " " + poss);
        }
        output4.close();
        File outPutFile5 = new File("out_data/5.txt");
        PrintWriter output5 = new PrintWriter(outPutFile5);
        for (Map.Entry<String,Integer> entry : m5.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            output5.println(entry.getKey() + " " + poss);
        }
        output5.close();
        File outPutFile6 = new File("out_data/6.txt");
        PrintWriter output6 = new PrintWriter(outPutFile6);
        for (Map.Entry<String,Integer> entry : m6.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            output6.println(entry.getKey() + " " + poss);
        }
        output6.close();
        File outPutFile7 = new File("out_data/7.txt");
        PrintWriter output7 = new PrintWriter(outPutFile7);
        for (Map.Entry<String,Integer> entry : m7.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            output7.println(entry.getKey() + " " + poss);
        }
        output7.close();
        File outPutFile8 = new File("out_data/8.txt");
        PrintWriter output8 = new PrintWriter(outPutFile8);
        for (Map.Entry<String,Integer> entry : m8.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            output8.println(entry.getKey() + " " + poss);
        }
        output8.close();
        File outPutFile9 = new File("out_data/9.txt");
        PrintWriter output9 = new PrintWriter(outPutFile9);
        for (Map.Entry<String,Integer> entry : m9.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            output9.println(entry.getKey() + " " + poss);
        }
        output9.close();
        File outPutFilefk = new File("out_data/useless.txt");
        PrintWriter outputfk = new PrintWriter(outPutFilefk);
        for (Map.Entry<String,Integer> entry : fk.entrySet()) {
            int co = entry.getValue();
            double poss = (double)co/total;
            outputfk.println(entry.getKey() + " " + poss);
        }
        outputfk.close();
    }
}
