#include<stdio.h>
#include<stdlib.h>
#include<string.h>
#include<assert.h>

struct Tree;
struct TreeLog;
struct MemoEntry;

/* local malloc */
static size_t cnez_used = 0;

static void *_malloc(size_t t)
{
    size_t *d = (size_t*)malloc(sizeof(size_t) + t);
    cnez_used += t;
    d[0] = t;
    memset((void*)(d+1), 0, t);
    return (void*)(d+1);
}

static void *_calloc(size_t items, size_t size)
{
    return _malloc(items * size);
}

static void _free(void *p)
{
    size_t *d = (size_t*)p;
    cnez_used -= d[-1];
    free(d-1);
}

/* Nez parser context */

typedef struct Nez {
    const unsigned char     *inputs;
    size_t                   length;
    const unsigned char     *pos;
    // AST
    struct Tree             *left;
    struct TreeLog          *logs;
    size_t                   log_size;
    size_t                   unused_log;
    // SymbolTable
    int                      count;
    // Memo
    struct MemoEntry        *memoArray;
    size_t                   memoSize;
    // GC
    struct Tree            **gcbuf;
    size_t                   gcbuf_size;
    size_t                   gcbuf_unused;
} Nez;

/* Tree */

typedef struct Tree {
    const char             *tag;
    const unsigned char    *text;
    size_t                  len;
    size_t                  size;
    const char            **labels;
    struct Tree           **childs;
} Tree;

/* TreeLog */

#define OpLink 0
#define OpTag  1
#define OpReplace 2
#define OpNew 3

typedef struct TreeLog {
    int op;
    void *value;
    struct Tree *tree;
} TreeLog;

static const char* ops[5] = {"link", "tag", "value", "new"};


/* memoization */

#define NotFound    0
#define SuccFound   1
#define FailFound   2

typedef long long int  uniquekey_t;

typedef struct MemoEntry {
    uniquekey_t  key;
    long         consumed;
    struct Tree *memoTree;
    int          result;
    int          stateValue;
} MemoEntry;

static Nez *Nez_new(const char *text, size_t len)
{
    Nez *c = (Nez*) _malloc(sizeof(Nez));
    c->inputs     = (const unsigned char*)text;
    c->length     = len;
    c->pos        = (const unsigned char*)text;
    c->left       = NULL;
    // tree
    c->log_size   = 256;
    c->logs       = (struct TreeLog*) _calloc(c->log_size, sizeof(struct TreeLog));
    c->unused_log = 0;
    // memo
    c->memoArray    = NULL;
    c->memoSize     = 0;
    // gcbuf
    c->gcbuf_size   = (4096 / sizeof(struct Tree*));
    c->gcbuf        = (struct Tree**) _calloc(c->gcbuf_size, sizeof(struct Tree*));
    c->gcbuf_unused = 0;
    return c;
}

/* Tree */

#define _ADHOC_UNUSED   (((size_t)1) << ((sizeof(void*)*8)-1))

static void _pushGCBuf(Nez *c, Tree *t)
{
    if(c->gcbuf_size == c->gcbuf_unused) {
        Tree **newbuf = (Tree **)_calloc(c->gcbuf_size * 2, sizeof(Tree *));
        memcpy(newbuf, c->gcbuf, sizeof(Tree *) * c->gcbuf_size);
        _free(c->gcbuf);
        c->gcbuf = newbuf;
        c->gcbuf_size *= 2;
    }
    c->gcbuf[c->gcbuf_size] = t;
    t->len |= _ADHOC_UNUSED;
    c->gcbuf_size++;
}

static Tree *_NEW(Nez *px, const char *tag, const unsigned char *text, size_t len, size_t n)
{
    Tree *t = (Tree*)_malloc(sizeof(struct Tree));
    t->tag  = tag;
    t->text = text;
    t->len  = len;
    t->size = n;
    if(n > 0) {
        t->labels = (const char**)_calloc(n, sizeof(const char*));
        t->childs = (Tree**)_calloc(n, sizeof(struct Tree*));
    }
    else {
        t->labels = NULL;
        t->childs = NULL;
    }
    _pushGCBuf(px, t);
    return t;
}

static Tree *Nez_token(Nez *px)
{
    Tree *t = (Tree*)_malloc(sizeof(struct Tree));
    t->tag  = NULL;
    t->text = px->inputs;
    t->len  = (px->pos - px->inputs);
    t->size = 0;
    t->labels = NULL;
    t->childs = NULL;
    return t;
}

static void _MARK(Tree *t)
{
    if(t != NULL) {
        size_t i;
        for(i = 0; i < t->size; i++) {
            _MARK(t->childs[i]);
        }
        t->len = t->len | ~(_ADHOC_UNUSED);
    }
}

static void _SWEEP(Nez *c)
{
    size_t i;
    for(i = 0; c->gcbuf_size; i++) {
        Tree *t = c->gcbuf[i];
        if((t->len & _ADHOC_UNUSED) == _ADHOC_UNUSED) {
            if(t->size > 0) {
                size_t i = 0;
                _free(t->labels);
                _free(t->childs);
            }
            _free(t);
        }
        c->gcbuf[i] = NULL;
    }
    c->gcbuf_size = 0;
}

static void Nez_freeTree(Tree *t)
{
    if(t != NULL) {
        if(t->size > 0) {
            size_t i = 0;
            for(i = 0; i < t->size; i++) {
                Nez_freeTree(t->childs[i]);
            }
            _free(t->labels);
            _free(t->childs);
        }
        _free(t);
    }
}

static void _LINK(Tree *parent, size_t n, const char *label, Tree *child)
{
    Tree *t = (Tree*)parent;
    t->labels[n] = label;
    t->childs[n] = (struct Tree*)child;
}

//static size_t cnez_count(void *v, size_t c)
//{
//    size_t i;
//    Tree *t = (Tree*)v;
//    if(t == NULL) {
//        return c+0;
//    }
//    c++;
//    for(i = 0; i < t->size; i++) {
//        c = cnez_count(t->childs[i], c);
//    }
//    return c;
//}
//static void cnez_dump_memory(const char *msg, void *t)
//{
//    size_t alive = cnez_count(t, 0);
//    size_t used = (t_newcount - t_gccount);
//    fprintf(stdout, "%s: tree=%ld[bytes], new=%ld, gc=%ld, alive=%ld %s\n", msg, t_used, t_newcount, t_gccount, alive, alive == used ? "OK" : "LEAK");
//}

/* API */

static int Nez_eof(Nez *c)
{
    return !(c->pos < (c->inputs + c->length));
}

static int Nez_eof2(Nez *c, size_t n) {
    if (c->pos + n <= c->inputs + c->length) {
        return 1;
    }
    return 0;
}

static const unsigned char Nez_read(Nez *c)
{
    return *(c->pos++);
}

static const unsigned char Nez_prefetch(Nez *c)
{
    return *(c->pos);
}

static void Nez_move(Nez *c, int shift)
{
    c->pos += shift;
}

static const unsigned char *Nez_pos(Nez *c)
{
    return c->pos;
}

static void Nez_setpos(Nez *c, const unsigned char *ppos)
{
    c->pos = ppos;
}

static int Nez_match(Nez *c, const unsigned char *text, size_t len) {
    if (c->pos + len > c->inputs + c->length) {
        return 0;
    }
    size_t i;
    for (i = 0; i < len; i++) {
        if (text[i] != c->pos[i]) {
            return 0;
        }
    }
    c->pos += len;
    return 1;
}

static int Nez_match2(Nez *c, const unsigned char c1, const unsigned char c2) {
    if (c->pos[0] == c1 && c->pos[1] == c2) {
        c->pos+=2;
        return 1;
    }
    return 0;
}

static int Nez_match3(Nez *c, const unsigned char c1, const unsigned char c2, const unsigned char c3) {
    if (c->pos[0] == c1 && c->pos[1] == c2 && c->pos[2] == c3) {
        c->pos+=3;
        return 1;
    }
    return 0;
}

static int Nez_match4(Nez *c, const unsigned char c1, const unsigned char c2, const unsigned char c3, const unsigned char c4) {
    if (c->pos[0] == c1 && c->pos[1] == c2 && c->pos[2] == c3 && c->pos[3] == c4) {
        c->pos+=4;
        return 1;
    }
    return 0;
}

static int Nez_match5(Nez *c, const unsigned char c1, const unsigned char c2, const unsigned char c3, const unsigned char c4, const unsigned char c5) {
    if (c->pos[0] == c1 && c->pos[1] == c2 && c->pos[2] == c3 && c->pos[3] == c4 && c->pos[4] == c5 ) {
        c->pos+=5;
        return 1;
    }
    return 0;
}

static int Nez_match6(Nez *c, const unsigned char c1, const unsigned char c2, const unsigned char c3, const unsigned char c4, const unsigned char c5, const unsigned char c6) {
    if (c->pos[0] == c1 && c->pos[1] == c2 && c->pos[2] == c3 && c->pos[3] == c4 && c->pos[4] == c5 && c->pos[5] == c6 ) {
        c->pos+=6;
        return 1;
    }
    return 0;
}

static int Nez_match7(Nez *c, const unsigned char c1, const unsigned char c2, const unsigned char c3, const unsigned char c4, const unsigned char c5, const unsigned char c6, const unsigned char c7) {
    if (c->pos[0] == c1 && c->pos[1] == c2 && c->pos[2] == c3 && c->pos[3] == c4 && c->pos[4] == c5 && c->pos[5] == c6 && c->pos[6] == c7) {
        c->pos+=7;
        return 1;
    }
    return 0;
}

static int Nez_match8(Nez *c, const unsigned char c1, const unsigned char c2, const unsigned char c3, const unsigned char c4, const unsigned char c5, const unsigned char c6, const unsigned char c7, const unsigned char c8) {
    if (c->pos[0] == c1 && c->pos[1] == c2 && c->pos[2] == c3 && c->pos[3] == c4 && c->pos[4] == c5 && c->pos[5] == c6 && c->pos[6] == c7 && c->pos[7] == c8) {
        c->pos+=8;
        return 1;
    }
    return 0;
}

// AST

static Tree* Nez_saveTree(Nez *c)
{
    return c->left;
}

static void Nez_backTree(Nez *c, Tree *left)
{
    c->left = left;
}

static void _log(Nez *c, int op, void *value, Tree *tree)
{
    if(!(c->unused_log < c->log_size)) {
        TreeLog *newlogs = (TreeLog *)_calloc(c->log_size * 2, sizeof(TreeLog));
        memcpy(newlogs, c->logs, c->log_size * sizeof(TreeLog));
        _free(c->logs);
        c->logs = newlogs;
        c->log_size *= 2;
    }
    TreeLog *l = c->logs + c->unused_log;
    l->op = op;
    l->value = value;
    assert(l->tree == NULL);
    l->tree  = tree;
    c->unused_log++;
}

void cnez_dump(void *v, FILE *fp);

static
void DEBUG_dumplog(Nez *c)
{
    long i;
    for(i = c->unused_log-1; i >= 0; i--) {
        TreeLog *l = c->logs + i;
        printf("[%d] %s %p ", (int)i, ops[l->op], l->value);
        if(l->tree != NULL) {
            cnez_dump(l->tree, stdout);
        }
        printf("\n");
    }
}

static void Nez_beginTree(Nez *c, int shift)
{
    _log(c, OpNew, (void *)(c->pos + shift), NULL);
}

static void Nez_linkTree(Nez *c, const char *label)
{
    _log(c, OpLink, (void*)label, c->left);
}

static void Nez_tagTree(Nez *c, const char *tag)
{
    _log(c, OpTag, (void*)tag, NULL);
}

static void Nez_valueTree(Nez *c, const char *text, size_t len)
{
    _log(c, OpReplace, (void*)text, (Tree*)len);
}

static void Nez_foldTree(Nez *c, int shift, const char* label)
{
    _log(c, OpNew, (void*)(c->pos + shift), NULL);
    _log(c, OpLink, (void*)label, c->left);
}

static size_t Nez_loadTreeLog(Nez *c)
{
    return c->unused_log;
}

static void Nez_storeTreeLog(Nez *c, size_t unused_log)
{
    if (unused_log < c->unused_log) {
        size_t i;
        for(i = unused_log; i < c->unused_log; i++) {
            TreeLog *l = c->logs + i;
            l->op = 0;
            l->value = NULL;
            l->tree = NULL;
        }
        c->unused_log = unused_log;
    }
}

static void Nez_endTree(Nez *c, int shift, const char* tag, const char *text, size_t len)
{
    int objectSize = 0;
    long i;
    for(i = c->unused_log - 1; i >= 0; i--) {
        TreeLog * l = c->logs + i;
        if(l->op == OpLink) {
            objectSize++;
            continue;
        }
        if(l->op == OpNew) {
            break;
        }
        if(l->op == OpTag && tag == 0) {
            tag = (const char*)l->value;
        }
        if(l->op == OpReplace) {
            if(text == NULL) {
                text = (const char*)l->value;
                len = (size_t)l->tree;
            }
            l->tree = NULL;
        }
    }
    TreeLog * start = c->logs + i;
    if(text == NULL) {
        text = (const char*)start->value;
        len = ((c->pos + shift) - (const unsigned char*)text);
    }
    Tree *t = _NEW(c, tag, (const unsigned char*)text, len, objectSize);
    c->left = t;
    if (objectSize > 0) {
        int n = 0;
        size_t j;
        for(j = i; j < c->unused_log; j++) {
            TreeLog * cur = c->logs + j;
            if (cur->op == OpLink) {
                _LINK(c->left, n++, (const char*)cur->value, cur->tree);
            }
        }
    }
    Nez_storeTreeLog(c, i);
}

// Counter -----------------------------------------------------------------

static void Nez_scanCount(Nez *c, const unsigned char *ppos, long mask, int shift)
{
    long i;
    size_t length = c->pos - ppos;
    if (mask == 0) {
        c->count = strtol((const char*)ppos, NULL, 10);
    } else {
        long n = 0;
        const unsigned char *p = ppos;
        while(p < c->pos) {
            n <<= 8;
            n |= (*p & 0xff);
            p++;
        }
        c->count = (n & mask) >> shift;
    }
}

static int Nez_decCount(Nez *c)
{
    return (c->count--) > 0;
}

// Memotable ------------------------------------------------------------

static
void Nez_initMemo(Nez *c, int w, int n)
{
    int i;
    c->memoSize = w * n + 1;
    c->memoArray = (MemoEntry *)_calloc(sizeof(MemoEntry), c->memoSize);
    for (i = 0; i < c->memoSize; i++) {
        c->memoArray[i].key = -1LL;
    }
}

static  uniquekey_t longkey( uniquekey_t pos, int memoPoint) {
    return ((pos << 16) | memoPoint);
}

static
int Nez_memoLookup(Nez *c, int memoPoint)
{
    uniquekey_t key = longkey((c->pos - c->inputs), memoPoint);
    unsigned int hash = (unsigned int) (key % c->memoSize);
    MemoEntry* m = c->memoArray + hash;
    if (m->key == key) {
        c->pos += m->consumed;
        return m->result;
    }
    return NotFound;
}

static
int Nez_memoLookupTree(Nez *c, int memoPoint)
{
    uniquekey_t key = longkey((c->pos - c->inputs), memoPoint);
    unsigned int hash = (unsigned int) (key % c->memoSize);
    MemoEntry* m = c->memoArray + hash;
    if (m->key == key) {
        c->pos += m->consumed;
        c->left = m->memoTree;
        return m->result;
    }
    return NotFound;
}

static
void Nez_memoSucc(Nez *c, int memoPoint, const unsigned char* ppos)
{
    uniquekey_t key = longkey((ppos - c->inputs), memoPoint);
    unsigned int hash = (unsigned int) (key % c->memoSize);
    MemoEntry* m = c->memoArray + hash;
    m->key = key;
    m->memoTree = c->left;
    m->consumed = c->pos - ppos;
    m->result = SuccFound;
    m->stateValue = -1;
}

static
void Nez_memoTreeSucc(Nez *c, int memoPoint, const unsigned char* ppos)
{
    uniquekey_t key = longkey((ppos - c->inputs), memoPoint);
    unsigned int hash = (unsigned int) (key % c->memoSize);
    MemoEntry* m = c->memoArray + hash;
    m->key = key;
    m->memoTree = c->left;
    m->consumed = c->pos - ppos;
    m->result = SuccFound;
    m->stateValue = -1;
}

static
void Nez_memoFail(Nez *c, int memoPoint)
{
    uniquekey_t key = longkey((c->pos - c->inputs), memoPoint);
    unsigned int hash = (unsigned int) (key % c->memoSize);
    MemoEntry* m = c->memoArray + hash;
    m->key = key;
    m->memoTree = c->left;
    m->consumed = 0;
    m->result = FailFound;
    m->stateValue = -1;
}

//----------------------------------------------------------------------------

static inline int Nez_bitis(Nez *c, int *bits, size_t n)
{
    return (bits[n / 32] & (1 << (n % 32))) != 0;
}

//----------------------------------------------------------------------------

static Tree* Nez_getTree(Nez *c)
{
    _MARK(c->left);
    _SWEEP(c);
    return c->left;
}

static void Nez_free(Nez *c)
{
    if(c->memoArray != NULL) {
        _free(c->memoArray);
        c->memoArray = NULL;
    }
    //Nez_backTreeLog(c, 0);
    _free(c->logs);
    c->logs = NULL;
    _free(c->gcbuf);
    c->gcbuf = NULL;
    _free(c);
}





