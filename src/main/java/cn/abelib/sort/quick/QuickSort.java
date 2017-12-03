package cn.abelib.sort.quick;

import cn.abelib.sort.BasicSort;

import java.util.Arrays;
import java.util.Collections;

/**
<<<<<<< HEAD
 * Created by abel-huang on 2017/7/18.
=======
 * Created by huangjinajin on 2017/7/18.
>>>>>>> 245eb44931ba0001767e05c615c1e630842b2ec3
 */
public class QuickSort extends BasicSort {
    @Override
    public void sort(Comparable[] a) {
        Collections.shuffle(Arrays.asList(a));
        sort(a, 0, a.length - 1);
    }


    private void sort(Comparable []a, int low, int high){
        if (low>=high)
            return;
        int p=partition(a, low, high);
        sort(a, low, p-1);
        sort(a, p+1, high);
    }

    private int partition(Comparable []a, int low, int high){
        Comparable value=a[low];
        int i=low, j=high+1;
        while (true){
            while(less(a[++i], value))
                if (i==high)
                    break;
            while(less(value, a[--j]))
                if (j==low)
                    break;
            if (i>=j)
                break;
            else
                exchange(a, i ,j);
        }
        exchange(a, low, j);
        return j;
    }
}