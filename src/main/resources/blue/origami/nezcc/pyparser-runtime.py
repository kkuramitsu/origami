
OpLink = 0
OpTag = 1
OpReplace = 2
OpNew = 3

class NezParser:
    def __init__(self, inputs, pos=0, newfunc = newtree, linkfunc = linktree):
        self.inputs = inputs + '\0'
        self.pos = pos;
        self.tree = None
        self.logs = None
        self.newfunc = newfunc
        self.linkfunc = linkfunc
    
    # PatternMatch
    def eof(self):
        return !(self.pos < len(inputs) - 1)

    def move(self, shift):
        pos += shift

    def read(self):
        self.pos + 1
        return self.inputs[pos]

    def prefetch(self):
        return self.inputs[pos]

    def match(self, text):
        TODO();
        self.pos += len(text)

    # Tree Construction

    def loadTreeLog(self):
        return self.logs

    def pushLog(self, op, value, tree):
        self.logs = ((op, value, tree), self.logs)

    def storeTreeLog(self, treeLogs):
        self.treeLogs = treeLogs

    def beginTree(self, shift):
        self.pushLog(OpNew, this.pos + shift, None)

    def linkTree(self, label):
        self.pushLog(OpLink, label, self.tree)

    def tagTree(self, tag):
        self.pushLog(OpTag, tag, None)

    def valueTree(self, text):
        self.pushLog(OpReplace, text, None)

    def foldTree(self, shift, label):
        self.pushLog(OpNew, self.pos + shift, None)
        self.pushLog(OpLink, label, self.tree)

    def endTree(self, shift, tag, value):
        spos = 0
        subTrees = []
        while(self.logs != None):
            log = self.logs[0]
            self.logs = self.logs[1]
            op = log[0]
            if(op == OpLink) :
                subTrees.append(log)
                continue
            if op == OpNew:
                spos = log[1]
                break
            if op == OpTag and tag == None:
                tag = log[1]
            if op == OpReplace and value == None:
                value = log[1]
        #end_while
        self.tree = self.newfunc(tag, self.inputs, spos, (self.pos + shift) - spos, len(subTrees), value)
        if len(subTrees) > 0:
            n = 0
            subTrees = subTrees.reverse()
            for(log in subTrees) :
                self.linkfunc(self.tree, n, log[1], log[2])
                n = n + 1

    # Memo
    def initMemo(self, w, n):
        memoSize = w * n + 1
        self.memo = []
        for n in range(memosize):
            self.memo.append(Memo())

    def longkey(self, memoPoint):
        return self.pos << 10 | memoPoint

    def memoSucc(self, memoPoint, ppos):
        key = self.longkey(memoPoint)
        hash = key % len(self.memo)
        m = self.memo[hash]
        m.key = key
        m.pos = ppos
        m.consumed = ppos - self.pos
        m.result = 1

    def memoFail(self, memoPoint):
        key = self.longkey(memoPoint)
        hash = key % len(self.memo)
        m = self.memo[hash]
        m.key = key
        m.pos = this.ppos
        m.consumed = 0
        m.result = 2

    def memoLookup(self, memoPoint):
        key = self.longkey(memoPoint)
        hash = key % len(self.memo)
        m = self.memo[hash]
        if m.key == key:
            self.pos += m.consumed
            return m.result
        return 0

    def memoSucc(self, memoPoint, ppos):
        key = self.longkey(memoPoint)
        hash = key % len(self.memo)
        m = self.memo[hash]
        m.key = key
        m.pos = ppos
        m.consumed = ppos - self.pos
        m.tree = self.tree
        m.result = 1

    def memoLookupTree(self, memoPoint):
        key = self.longkey(memoPoint)
        hash = key % len(self.memo)
        m = self.memo[hash]
        if m.key == key:
            self.pos += m.consumed
            self.tree = m.tree
            return m.result
        return 0

class Memo:
    def __init__(self):
        self.key = 0
        self.pos = 0
        self.tree = None
        self.state = None
        self.consumed = 0
        self.result = 0


class Tree:
    def __init__(self, tag, inputs, pos, len, size, value):
        self.tag = tag
        self.inputs = inputs
        self.pos = pos
        self.len = len;
        self.labels = [None for i in range(size)]
        self.childs = [None for i in range(size)]

    def link(self, index, label, child):
        self.labels[index] = label
        self.childs[index] = child

def newtree(tag, inputs, pos, len, size, value):
    return Tree(tag, inputs, pos, len, size, value)

def linktree(parent, index, label, child):
    parent.link(index, label, child)