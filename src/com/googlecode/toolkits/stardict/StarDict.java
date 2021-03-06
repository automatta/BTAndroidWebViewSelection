/*
  Copyright 2009 http://code.google.com/p/toolkits/. All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:
  * Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.
  * Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the following
  disclaimer in the documentation and/or other materials provided
  with the distribution.
  * Neither the name of http://code.google.com/p/toolkits/ nor the names of its
  contributors may be used to endorse or promote products derived
  from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.googlecode.toolkits.stardict;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.text.Normalizer;
import java.util.Formatter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.CacheLoader;
import java.util.concurrent.ExecutionException;
import android.util.Log;

public class StarDict implements StarDictInterface {

    static int packI(byte[] buffer, int offset) {
        return
            ((0XFF & buffer[offset]) << 24) +
            ((0XFF & buffer[offset+1]) << 16) +
            ((0XFF & buffer[offset+2]) << 8) +
            ((0XFF & buffer[offset+3]) << 0);
    }

    RandomAccessFile index;
    RandomAccessFile dict;
    RandomAccessFile ii; // index of index
    int mTotalEntries;
    int mDebug = 0;
    LoadingCache<String, Integer> mWordIdxCache = CacheBuilder.newBuilder().maximumSize(10000).build(new CacheLoader<String, Integer>() {
            public Integer load(String word) {
                return getWordIdxInternal(word);
            }
        });

    LoadingCache<Integer, String> mIdxWordCache = CacheBuilder.newBuilder().maximumSize(10000).build(new CacheLoader<Integer, String>() {
            public String load(Integer idx) {
                try {
                    return getWordInternal(idx);
                } catch (IOException e) {
                    return "";
                }
            }
        });

    void die(String s) {
        System.out.println(s);
        System.exit(-1);
    }

    private void debug(String format, Object... args) {
        if (mDebug != 0) {
            new Formatter(System.err).format(format, args);
        }
    }

    @Override
    public int getTotalNumOfEntries() {
        return mTotalEntries;
    }

    private boolean mHtmlize = false;
    private StarDict mDerivedDict = null;

    public StarDict(String dictname) {
        this(dictname, "");
    }

    public StarDict(String dictname, String derivedDictname) {
        if (!derivedDictname.isEmpty()) {
            mDerivedDict = new StarDict(derivedDictname);
        }


        try {
            if (new File(dictname + ".htmlize").exists()) {
                mHtmlize = true;
            }
            this.ii = new RandomAccessFile(dictname+".ii", "r");
            this.index = new RandomAccessFile(dictname+".idx", "r");
            this.dict = new RandomAccessFile(dictname+".dz", "r");

            mTotalEntries = (int) (new File(dictname+".ii").length()/4);
        }
        catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getWordIdx(String word) {
        try {
            int wordIdx = mWordIdxCache.get(word);
            String wordX = getWord(wordIdx);

            if (mDerivedDict != null && !normalNcaseEqual(word, wordX)) {
                int derivedIdx = mDerivedDict.getWordIdx(word);
                String derivedWordX = mDerivedDict.getWord(derivedIdx);

                if (normalNcaseEqual(word, derivedWordX)) {
                    ArrayList<String> words = mDerivedDict.getExplanation(word);
                    if (words.size() == 1) {
                        wordIdx = mWordIdxCache.get(words.get(0));
                    }
                }
            }

            return wordIdx;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return 0;
        }
    }
    private int getWordIdxInternal(String word) {

        try {
            return binarySearchHelper(getNormalWord(word), word, 0, mTotalEntries);
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int tryToFindExactMatch(String normalWord, String word, int mid) {
        do {
            mid--;
        } while (mid >= 0 && normalWord.compareToIgnoreCase(getNormalWord(getWord(mid))) == 0);

        do {
            mid++;
            if (word.compareTo(getWord(mid)) == 0) {
                return mid;
            }
        } while (normalWord.compareToIgnoreCase(getNormalWord(getWord(mid))) == 0);

        return mid - 1;
    }

    private int binarySearchHelper(String normalWord, String word, int min, int maxP1) throws IOException{
        if (min + 1 >= maxP1) {
            return min;
        }

        int mid = (min + maxP1) / 2;
        String midWord = getWord(mid);
        String midWordNormal = getNormalWord(midWord);
        debug("?: word is %s, midWord is %s, mid is %d, min is %d, max is %d\n", word, midWord, mid, min, maxP1);
        int compRes = normalWord.compareToIgnoreCase(midWordNormal);
        if (compRes == 0) {
            if (word.compareTo(midWord) == 0) {
                debug("ok: word is %s, midWord is %s, midWordNormal is %s, mid is %s\n", word, midWord, midWordNormal, mid);
                return mid;
            } else {
                return tryToFindExactMatch(normalWord, word, mid);
            }
        } else if ( compRes <= 0) {
            return binarySearchHelper(normalWord, word, min, mid);
        } else {
            return binarySearchHelper(normalWord, word, mid + 1, maxP1);
        }

    }

    private static String getNormalWord(String word) {
        StringBuilder b = new StringBuilder();

        for (char c : Normalizer.normalize(word, Normalizer.Form.NFKD).toCharArray()) {
            if (c < 128) {
                b.append(c);
            }
        }
        return b.toString();
    }

    @Override
    public String getWord(int idx) {
        try {
            return mIdxWordCache.get(idx);
        } catch (ExecutionException e) {
            return "";
        }
    }

    private String getWordInternal(int idx) throws IOException {
        if (idx < 0 || idx >= mTotalEntries) {
            return "";
        }

        int wordStart = 0;
        if (idx != 0) {
            wordStart = getEntryEndPos(idx - 1) + 1;
        }

        int wordEndPlus1 = getByte0Pos(idx);
        if (wordEndPlus1 <= wordStart) {
            return "";
        }

        byte[] buffer = new byte[wordEndPlus1 - wordStart];
        index.seek(wordStart);
        index.read(buffer);

        return new String(buffer, "UTF8");
    }

    private int getByte0Pos(int idx) throws IOException {
        ii.seek(idx * 4);
        int ret = ii.readInt();
        return ret - 1;
    }

    private int getEntryEndPos(int idx) throws IOException {
        int pos0 = getByte0Pos(idx);
        index.seek(pos0 + 1);
        int nDefs = index.readUnsignedByte();
        int ret = pos0 + 1 + nDefs * 8;
        return ret;
    }

    private ArrayList<Pair<Integer, Integer>> getStartEnds(int idx) {
        ArrayList<Pair<Integer, Integer>> start_ends = new ArrayList<Pair<Integer, Integer>>();

        try {
            int pos0 = getByte0Pos(idx);
            index.seek(pos0 + 1);

            int nDefs = index.readUnsignedByte();

            for (int i = 0; i < nDefs; i++) {
                int start = index.readInt();
                int end = index.readInt();

                start_ends.add(new Pair(start, end));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return start_ends;
    }

    static boolean normalNcaseEqual(String a, String b) {
        return getNormalWord(a).toLowerCase().equals(
               getNormalWord(b).toLowerCase());
    }

    public ArrayList<String> getExplanation(String word) {

        // search the ii for the word, should be a binary search

        ArrayList<Pair<Integer, Integer>> start_ends = null;

        int wordIdx = getWordIdx(word);
        String wordX = getWord(wordIdx);

        if (mDerivedDict != null && !normalNcaseEqual(word, wordX)) {
            int derivedIdx = mDerivedDict.getWordIdx(word);
            String derivedWordX = mDerivedDict.getWord(derivedIdx);

            if (normalNcaseEqual(word, derivedWordX)) {
                ArrayList<String> words = mDerivedDict.getExplanation(word);
                if (words.size() > 1) {
                    return words;
                } else if (words.size() == 1) {
                    wordIdx = getWordIdx(words.get(0));
                }
            }
        }
        start_ends = getStartEnds(wordIdx);


        if (start_ends != null) {
            ArrayList<String> ret = new ArrayList<String>();
            for (Pair<Integer, Integer> p : start_ends) {
                int start = (int) p.first;
                int end = (int) p.second;

                byte[] buffer = new byte[end - start];

                try {
                    dict.seek(start);
                    dict.readFully(buffer, 0, buffer.length);
                    String add = new String(buffer, "UTF8");

                    if (mHtmlize) {
                        add = String.format("<pre>%s</pre><hr>", add);
                    }
                    ret.add(add);
                } catch (Exception e) {
                    System.out.printf("start for %s is %d, stop is %d\n", word, start, end);
                    e.printStackTrace();
                }
            }
            return ret;
        }
        return null;
    }

    public static void main(String[] args) {
        StarDict dict = new StarDict("words");
        dict.mDebug = 1;
        for (String a : args) {
            for (String s : dict.getExplanation(a)) {
                System.out.println(s);
            }
        }
    }
}

class Pair<F, S> {
    public final F first;
    public final S second;

    /**
     * Constructor for a Pair. If either are null then equals() and hashCode() will throw
     * a NullPointerException.
     * @param first the first object in the Pair
     * @param second the second object in the pair
     */
    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Checks the two objects for equality by delegating to their respective equals() methods.
     * @param o the Pair to which this one is to be checked for equality
     * @return true if the underlying objects of the Pair are both considered equals()
     */
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Pair)) return false;
        final Pair<F, S> other;
        try {
            other = (Pair<F, S>) o;
        } catch (ClassCastException e) {
            return false;
        }
        return first.equals(other.first) && second.equals(other.second);
    }

    /**
     * Compute a hash code using the hash codes of the underlying objects
     * @return a hashcode of the Pair
     */
    public int hashCode() {
        int result = 17;
        result = 31 * result + first.hashCode();
        result = 31 * result + second.hashCode();
        return result;
    }

    /**
     * Convenience method for creating an appropriately typed pair.
     * @param a the first object in the Pair
     * @param b the second object in the pair
     * @return a Pair that is templatized with the types of a and b
     */
    public static <A, B> Pair <A, B> create(A a, B b) {
        return new Pair<A, B>(a, b);
    }
}
