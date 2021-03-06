package cn.abelib.datastructure.tree.bpt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author abel-huang
 * @date 2017/11/2
 * B+树的简单实现
 */
public class BalancePlusTree {
    /**
     * 根节点
     */
    private TreeNode root;
    /**
     * B+树阶数
     */
    private int degree;

    public BalancePlusTree(int degree) {
        this.degree = degree;
        this.root = null;
    }

    /**
     * 返回B+树的维度
     * @return
     */
    public int getDegree() {
        return this.degree;
    }

    public int getCeil() {
        return (int) Math.ceil(getDegree() / 2.0);
    }

    /**
     * 返回根节点
     * @return
     */
    public TreeNode getRoot() {
        return this.root;
    }


    /**
     * 对外暴露的接口
     * @param key
     */
    public void insert(String key) {
        insert(new KeyValue(key, null));
    }

    /**
     * 对外暴露的接口
     * @param key
     * @param value
     */
    public void insert(String key, Object value) {
        insert(new KeyValue(key, value));
    }

    /**
     * 对外暴露的接口
     * @param key
     * @param values
     */
    public void insert(String key, List<Object> values) {
        insert(new KeyValue(key, values));
    }

    /**
     * 插入操作
     * @param entry
     */
    private void insert(KeyValue entry) {
        // 插入空B+树，此时根节点也为空
        if (getRoot() == null) {
            TreeNode node = new TreeNode();
            node.getKeyValues().add(entry);
            this.root = node;
            // 根节点没有父节点
            this.root.setParentNode(null);
        }
        // 只有一个非空节点的B+树，此时仅有一个节点即根节点
        else if (getRoot().getChildren().isEmpty() && getRoot().getKeyValues().size() < (getDegree() - 1)) {
            insert(entry, getRoot());
        }
        // 常规场景
        else {
            TreeNode curr = getRoot();
            while (!curr.getChildren().isEmpty()) {
                curr = curr.getChildren().get(binarySearchInternalNode(entry, curr.getKeyValues()));
            }
            insert(entry, curr);
            // 如果超过阈值需要进行节点分裂
            if (curr.getKeyValues().size() == getDegree()) {
                split(curr);
            }
        }
    }

    /**
     * 进行节点的分裂
     * @param node
     */
    private void split(TreeNode node) {
        int mid = getDegree() / 2;
        TreeNode middleNode = new TreeNode();
        TreeNode rightNode = new TreeNode();

        // 分裂后的右边节点
        rightNode.setKeyValues(new ArrayList<>(node.getKeyValues().subList(mid, node.getKeyValues().size())));
        rightNode.setParentNode(middleNode);

        // 分裂后的中间节点
        middleNode.getKeyValues().add(new KeyValue(node.getKeyValues().get(mid)));
        middleNode.getChildren().add(rightNode);

        // 分裂之前的原始节点, 即分裂后左边的部分
        node.setKeyValues(new ArrayList<>(node.getKeyValues().subList(0, mid)));

        split(node.getParentNode(), node, middleNode, true);
    }

    /**
     *
     * @param curr 当前结点
     * @param prev 当前结点的子节点(实际上前驱节点)
     * @param insertingNode
     * @param first
     */
    private void split(TreeNode curr, TreeNode prev, TreeNode insertingNode, boolean first) {
        if (curr == null) {
            this.root = insertingNode;
            int indexForPrev = binarySearchInternalNode(prev.getKeyValues().get(0), insertingNode.getKeyValues());
            // 设置 insertingNode 父子节点关系
            prev.setParentNode(insertingNode);
            insertingNode.getChildren().add(indexForPrev, prev);
            if (first) {
                if (indexForPrev == 0) {
                    insertingNode.getChildren().get(0).setNextNode(insertingNode.getChildren().get(1));
                    insertingNode.getChildren().get(1).setPrevNode(insertingNode.getChildren().get(0));
                } else {
                    insertingNode.getChildren().get(indexForPrev - 1).setNextNode(insertingNode.getChildren().get(indexForPrev));
                    insertingNode.getChildren().get(indexForPrev + 1).setPrevNode(insertingNode.getChildren().get(indexForPrev));
                }
            }
        }
        else {
            promote(insertingNode, curr);
            // 如果合并后的节点已经满了
            if (curr.getKeyValues().size() == getDegree()) {
                int mid = (int) (Math.ceil(getDegree() / 2.0) - 1);
                TreeNode middleNode = new TreeNode();
                TreeNode rightNode = new TreeNode();

                // 分裂后的右边节点
                rightNode.setKeyValues(new ArrayList<>((curr.getKeyValues().subList(mid + 1, curr.getKeyValues().size()))));
                rightNode.setParentNode(middleNode);

                // 分裂后的中间节点
                middleNode.getKeyValues().add(curr.getKeyValues().get(mid));
                middleNode.getChildren().add(rightNode);

                List<TreeNode> childrenOfCurr = curr.getChildren();
                List<TreeNode> childrenOfRight = new ArrayList<>();
                int lastChildOfLeft = childrenOfCurr.size() - 1;

                for (int i = childrenOfCurr.size() - 1; i >= 0; i --) {
                    List<KeyValue> keyValues = childrenOfCurr.get(i).getKeyValues();
                    if (middleNode.getKeyValues().get(0).compareTo(keyValues.get(0)) <= 0) {
                        childrenOfCurr.get(i).setParentNode(rightNode);
                        childrenOfRight.add(0, childrenOfCurr.get(i));
                        lastChildOfLeft --;
                    } else {
                        break;
                    }

                }
                rightNode.setChildren(childrenOfRight);

                curr.getChildren().subList(lastChildOfLeft + 1, childrenOfCurr.size()).clear();
                curr.getKeyValues().subList(mid, curr.getKeyValues().size()).clear();

                split(curr.getParentNode(), curr, middleNode, false);
            }
        }
    }

    /**
     * 合并内部节点, 符合条件的数据节点晋升为索引节点
     * @param mergeFrom
     * @param mergeInto
     */
    private void promote(TreeNode mergeFrom, TreeNode mergeInto) {
        KeyValue keyValue = mergeFrom.getKeyValues().get(0);
        TreeNode childNode = mergeFrom.getChildren().get(0);

        int index = binarySearchInternalNode(keyValue, mergeInto.getKeyValues());
        int childIndex = index;
        if (keyValue.compareTo(childNode.getKeyValues().get(0)) <= 0) {
            childIndex = index + 1;
        }

        childNode.setParentNode(mergeInto);
        mergeInto.getChildren().add(childIndex, childNode);
        mergeInto.getKeyValues().add(index, keyValue);

        //
        if (!mergeInto.getChildren().isEmpty() && mergeInto.getChildren().get(0).getChildren().isEmpty()) {
            if (mergeInto.getChildren().size() - 1 != childIndex && mergeInto.getChildren().get(childIndex + 1).getPrevNode() == null) {
                mergeInto.getChildren().get(childIndex + 1).setPrevNode(mergeInto.getChildren().get(childIndex));
                mergeInto.getChildren().get(childIndex).setNextNode(mergeInto.getChildren().get(childIndex + 1));
            }

            else if (childIndex != 0 && mergeInto.getChildren().get(childIndex - 1).getNextNode() == null) {
                mergeInto.getChildren().get(childIndex).setPrevNode(mergeInto.getChildren().get(childIndex - 1));
                mergeInto.getChildren().get(childIndex - 1).setNextNode(mergeInto.getChildren().get(childIndex));
            }

            else {
                mergeInto.getChildren().get(childIndex).setNextNode(mergeInto.getChildren().get(childIndex - 1).getNextNode());
                mergeInto.getChildren().get(childIndex).getNextNode().setPrevNode(mergeInto.getChildren().get(childIndex));

                mergeInto.getChildren().get(childIndex - 1).setNextNode(mergeInto.getChildren().get(childIndex));
                mergeInto.getChildren().get(childIndex).setPrevNode(mergeInto.getChildren().get(childIndex - 1));
            }
        }
    }

    private void insert(KeyValue entry, TreeNode node) {
        int indexOfKey = binarySearchInternalNode(entry, node.getKeyValues());
        if (indexOfKey != 0 && node.getKeyValues().get(indexOfKey - 1).getKey().equals(entry.getKey())) {
            // 目前设计思路是key对应的value不是唯一的
            node.getKeyValues().get(indexOfKey - 1).getValues().addAll(entry.getValues());
        } else {
            node.getKeyValues().add(indexOfKey, entry);
        }
    }

    /**
     * 按照keyValues从小到大的顺序，找到entry所处的位置
     * @param entry
     * @param keyValues
     * @return
     */
    public int binarySearchInternalNode(KeyValue entry, List<KeyValue> keyValues) {
        int left = 0;
        int right = keyValues.size() - 1;
        int mid;
        int index = -1;
        // 如果小于第一个元素，那位置就在第一个位置
        if (entry.compareTo(keyValues.get(left)) < 0) {
            return 0;
        }
        // 如果大于最后一个元素，那位置就在集合最后一个位置
        if (entry.compareTo(keyValues.get(right)) >= 0) {
            return keyValues.size();
        }
        while (left <= right) {
            mid = left + (right - left) / 2;
            if (entry.compareTo(keyValues.get(mid)) < 0 && entry.compareTo(keyValues.get(mid - 1)) >= 0) {
                index = mid;
                break;
            } else if (entry.compareTo(keyValues.get(mid)) >= 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return index;
    }

    /**
     * 标准的二分搜索，没有则返回-1
     * @param entry
     * @param keyValues
     * @return
     */
    public int binarySearchInternalNode2(KeyValue entry, List<KeyValue> keyValues) {
        int left = 0;
        int right = keyValues.size() - 1;
        int mid;
        int index = -1;
        while (left <= right) {
            mid = left + (right - left) / 2;
            if (entry.compareTo(keyValues.get(mid)) == 0) {
                index = mid;
                break;
            } else if (entry.compareTo(keyValues.get(mid)) >= 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return index;
    }

    /**
     * 搜索操作
     * @param key
     * @return
     */
    public KeyValue search(String key) {
        KeyValue searchKey = new KeyValue(key);
        TreeNode curr = getRoot();
        while (!curr.getChildren().isEmpty()) {
            curr = curr.getChildren().get(binarySearchInternalNode(searchKey, curr.getKeyValues()));
        }

        for (KeyValue kv: curr.getKeyValues()) {
            if (kv.compareTo(searchKey) == 0) {
                return kv;
            } else if (kv.compareTo(searchKey) > 0) {
                break;
            }
        }

        return null;
    }

    /**
     * 查看键是否存在
     * @param key
     * @return
     */
    public boolean contains(String key) {
        return Objects.nonNull(search(key));
    }

    /**
     * scan操作，闭区间
     * @param from
     * @param to
     * @return
     */
    public List<KeyValue> scan(String from, String to) {
        KeyValue fromKey = new KeyValue(from);
        KeyValue toKey = new KeyValue(to);
        TreeNode curr = getRoot();
        while (!curr.getChildren().isEmpty()) {
            curr = curr.getChildren().get(binarySearchInternalNode(fromKey, curr.getKeyValues()));
        }

        List<KeyValue> keyValues = new ArrayList<>();
        boolean end = false;
        while (curr != null && !end) {
            for (KeyValue kv: curr.getKeyValues()) {
                if (kv.compareTo(fromKey) >= 0 && kv.compareTo(toKey) <= 0) {
                    keyValues.add(kv);
                } else if (kv.compareTo(toKey) > 0){
                    end = true;
                }
            }
            curr = curr.getNextNode();

        }
        return keyValues;
    }

    /**
     * 删除操作
     * @param key
     * @return
     */
    public boolean delete(String key) {
        KeyValue searchKey = new KeyValue(key);

        // 对于空B+树的删除，此时根节点为空
        if (getRoot() == null) {
            return false;
        }

        TreeNode curr = getRoot();
        // 只有一个根节点的B+树
        if (getRoot().getChildren().isEmpty()) {
            int index = binarySearchInternalNode2(searchKey, curr.getKeyValues());
            if (index < 0) {
                return false;
            }
            curr.getKeyValues().remove(index);
            return true;
        }
        // 一般场景
        else {
            while (!curr.getChildren().isEmpty()) {
                curr = curr.getChildren().get(binarySearchInternalNode(searchKey, curr.getKeyValues()));
            }
            int index = binarySearchInternalNode2(searchKey, curr.getKeyValues());
            if (index < 0) {
                return false;
            }
            // 进行删除操作
            curr.getKeyValues().remove(index);

            // 如果删除后的节点 >= (int) (Math.ceil(getDegree() / 2.0) - 1), 删除操作结束，否则需要进行后续合并操作
            if (curr.getKeyValues().size() < (int) Math.ceil(getDegree() / 2.0) - 1) {
                delete(curr);
            }
            return true;
        }
    }

    /**
     * 删除后的合并操作, 只考虑其与左边的兄弟节点之间的关系
     */
    private void delete(TreeNode curr) {
        TreeNode parent = curr.getParentNode();
        int childrenSize = parent.getChildren().size();
        // 获得当前节点的位于父节点的位置
        int index = binarySearchInternalNode(curr.getKeyValues().get(0), parent.getKeyValues());
        //  被删除的节点位于父节点的最左边, 那么需要考虑与右边节点进行合并
        if (index == 0 && childrenSize > 0) {
            TreeNode rightNode =  parent.getChildren().get(1);
            mergeExternalNode2(parent, curr, rightNode, index);
        }
        // 被删除的节点位于父节点的最右边，需要与左边进行合并
        else if (index == childrenSize - 1 && childrenSize > 0) {
            TreeNode leftNode =  parent.getChildren().get(index - 1);
            mergeExternalNode(parent, leftNode, curr, index);
        }
        // 被删除的节点需要分别与左边和右边分别进行对比
        else if (index > 0 && index < childrenSize - 1) {
            // 需要获得当前节点的左兄弟节点
            TreeNode leftNode =  parent.getChildren().get(index - 1);
            TreeNode rightNode =  parent.getChildren().get(index + 1);
            // 优先从左边节点借
            if (leftNode.getKeyValues().size() > (int) (Math.ceil(getDegree() / 2.0) - 1)) {
                borrowFromLeft(parent, leftNode, curr, index);
            } else {
                // 然后考虑从右边节点借
                if (rightNode.getKeyValues().size() > (int) (Math.ceil(getDegree() / 2.0) - 1)) {
                    borrowFromRight(parent, curr, rightNode, index);
                } else {
                    // 如果都不满足，则考虑和左边节点合并w
                    mergeLeftAndRight(parent, leftNode, curr, index);
                }
            }
        }

        if (parent.getKeyValues().size() >= (int) (Math.ceil(getDegree() / 2.0) - 1)) {
            return;
        }
        // 针对索引节点的合并
        mergeInternalNode(parent);
    }

    /**
     * 从左兄弟节点中借最后一个key，并删除右边key
     * @param leftNode
     * @param rightNode
     */
    public void borrowFromLeft(TreeNode parent, TreeNode leftNode, TreeNode rightNode, int index) {
        KeyValue borrowKeyValue = leftNode.getKeyValues().remove(leftNode.getKeyValues().size() - 1);
        rightNode.getKeyValues().add(0, borrowKeyValue);
        parent.getKeyValues().set(index - 1, borrowKeyValue);
    }

    /**
     * 从右兄弟节点中借第一个key，并删除左边key
     * @param parent
     * @param leftNode
     * @param rightNode
     */
    public void borrowFromRight(TreeNode parent, TreeNode leftNode, TreeNode rightNode, int index) {
        KeyValue borrowKeyValue = rightNode.getKeyValues().remove(0);
        leftNode.getKeyValues().add(borrowKeyValue);
        parent.getKeyValues().set(index, rightNode.getKeyValues().get(0));
    }

    /**
     * 合并左右数据节点，并删除索引中的数据
     * 需要从右边借一个节点
     * @param parent
     * @param leftNode
     * @param rightNode
     */
    public void mergeExternalNode(TreeNode parent, TreeNode leftNode, TreeNode rightNode, int index) {
        // 如果兄弟节点过半阈值，则需要借一个key
        if (leftNode.getKeyValues().size() > (int) (Math.ceil(getDegree() / 2.0) - 1)) {
            borrowFromLeft(parent, leftNode, rightNode, index);
        } else {
            // 其他情形，无法从兄弟节点借到数据, 则需要将左右节点进行合并
            mergeLeftAndRight(parent, leftNode, rightNode, index);
        }
    }

    public void mergeExternalNode2(TreeNode parent, TreeNode leftNode, TreeNode rightNode, int index) {
        // 如果兄弟节点过半阈值，则需要借一个key
        if (rightNode.getKeyValues().size() > (int) (Math.ceil(getDegree() / 2.0) - 1)) {
            borrowFromRight(parent, leftNode, rightNode, index);
        } else {
            mergeLeftAndRight(parent, leftNode, rightNode, index);
        }
    }

    public void mergeLeftAndRight(TreeNode parent, TreeNode leftNode, TreeNode rightNode, int index) {
        // 其他情形，无法从兄弟节点借到数据, 则需要将左右节点进行合并
        leftNode.getKeyValues().addAll(rightNode.getKeyValues());
        leftNode.setNextNode(rightNode.getNextNode());
        parent.getChildren().remove(index);
        parent.getKeyValues().remove(index - 1);
    }

    /**
     * 针对索引节点的合并
     * @param curr
     */
    private void mergeInternalNode(TreeNode curr) {
        TreeNode parent = curr.getParentNode();
        // 当该节点为根节点时，直接退出
        if (parent == null) {
            return;
        }
        int childrenSize = parent.getChildren().size();
        // 获得当前节点的位于父节点的位置
        int index = binarySearchInternalNode(curr.getKeyValues().get(0), parent.getKeyValues());

        //  被删除的节点位于父节点的最左边, 那么需要考虑与右边节点进行合并
        if (index == 0 && childrenSize > 0) {
            TreeNode rightNode =  parent.getChildren().get(1);
            mergeInternalNode2(parent, curr, rightNode, index);
        }
        // 被删除的节点位于父节点的最右边，需要与左边进行合并
        else if (index == childrenSize - 1 && childrenSize > 0) {
            TreeNode leftNode =  parent.getChildren().get(index - 1);
            mergeInternalNodeWith1(parent, leftNode, curr, index);
        }
        // 被删除的节点需要分别与左边和右边分别进行对比
        else if (index > 0 && index < childrenSize - 1) {
            // 需要获得当前节点的左兄弟节点
            TreeNode leftNode =  parent.getChildren().get(index - 1);
            TreeNode rightNode =  parent.getChildren().get(index + 1);
            // 优先从左边节点借
            if (leftNode.getKeyValues().size() > (int) (Math.ceil(getDegree() / 2.0) - 1)) {
                borrowFromLeft(parent, leftNode, curr, index);
            } else {
                // 然后考虑从右边节点借
                if (rightNode.getKeyValues().size() > (int) (Math.ceil(getDegree() / 2.0) - 1)) {
                    borrowFromRight(parent, curr, rightNode, index);
                } else {
                    // 如果都不满足，则考虑和左边节点合并
                    parent = merge(parent, leftNode, curr);
                }
            }
        }

        if (parent.getKeyValues().size() >= (int) (Math.ceil(getDegree() / 2.0) - 1)) {
            return;
        }
        // 针对索引节点的合并
        mergeInternalNode(parent);
    }

    /**
     * 合并左右数据节点，并删除索引中的数据
     * 需要从右边借一个节点
     * @param parent
     * @param leftNode
     * @param rightNode
     */
    public void mergeInternalNodeWith1(TreeNode parent, TreeNode leftNode, TreeNode rightNode, int index) {
        // 如果兄弟节点过半阈值，则需要借一个key
        if (leftNode.getKeyValues().size() > (int) (Math.ceil(getDegree() / 2.0) - 1)) {
            borrowFromLeft(parent, leftNode, rightNode, index);
        } else {
            // 其他情形，无法从兄弟节点借到数据, 则需要将左右节点和父节点进行合并
            merge(parent, leftNode, rightNode);
        }
    }

    public void mergeInternalNode2(TreeNode parent, TreeNode leftNode, TreeNode rightNode, int index) {
        // 如果兄弟节点过半阈值，则需要借一个key
        if (rightNode.getKeyValues().size() > (int) (Math.ceil(getDegree() / 2.0) - 1)) {
            borrowFromRight(parent, leftNode, rightNode, index);
        } else {
            // 无法从兄弟节点借到数据, 则需要将左右节点和父节点进行合并
            merge(parent, leftNode, rightNode);
        }
    }

    /**
     * 将左右节点和根节点合并, 并更新节点
     * @param parent
     * @param leftNode
     * @param rightNode
     */
    public TreeNode merge(TreeNode parent, TreeNode leftNode, TreeNode rightNode) {
        // parent 作为合并后的新节点
        parent.getKeyValues().addAll(0, leftNode.getKeyValues());
        parent.getKeyValues().addAll(rightNode.getKeyValues());

        parent.getChildren().clear();
        parent.getChildren().addAll(leftNode.getChildren());
        parent.getChildren().addAll(rightNode.getChildren());
        return parent;
    }

    /**
     * 更新接口
     * @param key
     * @param value
     * @return
     */
    public boolean update(String key, Object value) {
        return update(new KeyValue(key, value));
    }

    public boolean update(String key, List<Object> values) {
        return update(new KeyValue(key, values));
    }

    private boolean update(KeyValue entry) {
        TreeNode curr = getRoot();
        while (!curr.getChildren().isEmpty()) {
            curr = curr.getChildren().get(binarySearchInternalNode(entry, curr.getKeyValues()));
        }

        for (KeyValue kv: curr.getKeyValues()) {
            if (kv.compareTo(entry) == 0) {
                kv.setValues(entry.getValues());
                return true;
            } else if (kv.compareTo(entry) > 0) {
                insert(entry);
                return false;
            }
        }

        return false;
    }

    /**
     * @return
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("BalancePlusTree{" +
                "root=" + root +
                ", degree=" + degree +
                ", ");

        for (KeyValue keyValue : this.toList()) {
            builder.append(keyValue);
        }
        builder.append( '}');
        return builder.toString();
    }

    public List<KeyValue> toList() {
        List<KeyValue> list = new ArrayList<>();
        TreeNode curr = this.root;
        while (curr != null) {
            if (!curr.getChildren().isEmpty()) {
                curr = curr.getChildren().get(0);
            } else {
                break;
            }
        }
        while (curr != null) {
            list.addAll(curr.getKeyValues());
            curr = curr.getNextNode();
        }
        return list;
    }
}
