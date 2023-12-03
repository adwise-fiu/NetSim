package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author fatihsenel
 * date: 20.09.23
 */
public class Permutations {
    public static void main(String[] args) {
        int n = 10; // Change n to the desired value
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) {
            nums[i] = i;
        }
        long t = System.currentTimeMillis();
        List<int[]> per = new ArrayList<>();
        per = generatePermutations(nums);
        System.out.println(System.currentTimeMillis() - t);
        System.out.println();
    }

    // Recursive function to generate permutations
    public static void generatePermutations(List<int[]> list, int[] nums, int left, int right) {
        if (left == right) {
            // A permutation is generated, so print it
//            System.out.println(Arrays.toString(nums));
            list.add(nums);
        } else {
            for (int i = left; i <= right; i++) {
                // Swap nums[left] with nums[i]
                swap(nums, left, i);

                // Recursively generate permutations for the remaining elements
                generatePermutations(list, nums, left + 1, right);

                // Undo the swap to backtrack and try the next possibility
                swap(nums, left, i);
            }
        }
    }

    public static List<int[]> generatePermutations(int[] nums) {
        List<int[]> ans = new ArrayList<>();




        int n = nums.length;
        while (true) {
            // Print the current permutation
            ans.add(nums.clone());

            // Find the largest index i such that nums[i] < nums[i+1]
            int k = n - 2;
            while (k >= 0 && nums[k] >= nums[k + 1]) {
                k--;
            }

            // If no such index exists, all permutations have been generated
            if (k < 0) {
                break;
            }

            // Find the largest index j such that nums[j] > nums[i]
            int j = n - 1;
            while (nums[j] <= nums[k]) {
                j--;
            }

            // Swap nums[i] and nums[j]
            swap(nums, k, j);

            // Reverse the subarray nums[i+1...n-1]
            reverse(nums, k + 1, n - 1);
        }
        return ans;
    }

    public static void reverse(int[] nums, int start, int end) {
        while (start < end) {
            swap(nums, start, end);
            start++;
            end--;
        }
    }

    // Utility function to swap two elements in an array
    public static void swap(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }
}
