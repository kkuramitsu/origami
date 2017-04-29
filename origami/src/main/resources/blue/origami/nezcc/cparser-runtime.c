#include<stdio.h>
#include<stdlib.h>
#include<string.h>
#include<assert.h>

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

typedef const unsigned char pchar;
typedef void* (*NewFunc)(const char *, const char *, size_t, size_t, size_t, const char *, void *); 
typedef void* (*SetFunc)(void *, size_t n, const char *, void *child, void *); 

typedef struct Nez {
    pchar                   *inputs;
    size_t                   length;
    pchar                   *pos;
    // AST
    void                    *tree;
    struct TreeLog          *logs;
    size_t                   logsize;
    size_t                   treeLog;
    // GC
	NewFunc                  newfunc;
	SetFunc                  setfunc;
	void                    *thunk;
    // SymbolTable
    void                    *state;
    // Memo
    struct MemoEntry        *memoArray;
    size_t                   memoSize;
} NezParserContext;

/* TreeLog */

#define OpLink 0
#define OpTag  1
#define OpReplace 2
#define OpNew 3

typedef struct TreeLog {
    int op;
    void *value;
    void *tree;
} TreeLog;

static const char* ops[5] = {"link", "tag", "value", "new"};

/* memoization */

#define NotFound    0
#define SuccFound   1
#define FailFound   2

typedef long long int  ukey_t;

typedef struct MemoEntry {
    ukey_t        key;
    long          consumed;
    struct Tree  *memoTree;
    int           result;
    void         *state;
} MemoEntry;

/* NezParserContext */

static void *Tree_new(const char *tag, const char *inputs, size_t pos, size_t len, size_t nsubs, const char *value, void *thunk);
static void* Tree_set(void *parent, size_t n, const char *label, void *child, void *thunk);
static void initMemo(NezParserContext *px, int w, int n);

static NezParserContext *NezParserContext_new(const char *text, size_t len, int memoSize)
{
    NezParserContext *px = (NezParserContext*) _malloc(sizeof(NezParserContext));
    px->inputs     = (pchar*)text;
    px->length     = len;
    px->pos        = (pchar*)text;
    // tree
    px->tree       = NULL;
    px->logsize    = 256;
    px->logs       = (struct TreeLog*) _calloc(px->logsize, sizeof(struct TreeLog));
    px->treeLog    = 0;
	px->newfunc    = Tree_new;
	px->setfunc    = Tree_set;
	px->thunk      = NULL; 	
    // memo
    px->memoArray    = NULL;
    px->memoSize     = 0;
    initMemo(px, 64, memoSize);
    return px;
}

/* API */

static inline int _eof(NezParserContext *px)
{
    return !(px->pos < (px->inputs + px->length));
}

static inline pchar _read(NezParserContext *px)
{
    return *(px->pos++);
}

static int _is(NezParserContext *px, pchar ch)
{
    return *(px->pos++) == ch;
}

static inline pchar _getbyte(NezParserContext *px)
{
    return *(px->pos);
}

static inline int _move(NezParserContext *px, int shift)
{
    px->pos += shift;
    return 1;
}

static int _matchBytes(NezParserContext *px, const char *text, size_t len) {
    if (px->pos + len > px->inputs + px->length) {
        return 0;
    }
    size_t i;
    for (i = 0; i < len; i++) {
        if ((pchar)text[i] != px->pos[i]) {
            return 0;
        }
    }
    px->pos += len;
    return 1;
}

static inline int _back(NezParserContext *px, pchar *ppos)
{
    px->pos = ppos;
    return 1;
}

static inline int _backS(NezParserContext *px, void *state)
{
    px->state = state;
    return 1;
}

// Tree Construction

static inline int _backT(NezParserContext *px, void *tree)
{
    px->tree = tree;
    return 1;
}

static void _log(NezParserContext *px, int op, void *value, void *tree)
{
    if(!(px->treeLog < px->logsize)) {
        TreeLog *newlogs = (TreeLog *)_calloc(px->logsize * 2, sizeof(TreeLog));
        memcpy(newlogs, px->logs, px->logsize * sizeof(TreeLog));
        _free(px->logs);
        px->logs = newlogs;
        px->logsize *= 2;
    }
    TreeLog *l = px->logs + px->treeLog;
    l->op = op;
    l->value = value;
    l->tree  = tree;
    px->treeLog++;
}

void cnez_dump(void *v, FILE *fp);

static void DEBUG_dumplog(NezParserContext *px)
{
    long i;
    for(i = px->treeLog-1; i >= 0; i--) {
        TreeLog *l = px->logs + i;
        printf("[%d] %s %p ", (int)i, ops[l->op], l->value);
        if(l->tree != NULL) {
            cnez_dump(l->tree, stdout);
        }
        printf("\n");
    }
}

static int _BoT(NezParserContext *px, int shift)
{
    _log(px, OpNew, (void *)(px->pos + shift), NULL);
    return 1;
}

static int _link(NezParserContext *px, const char *label)
{
    _log(px, OpLink, (void*)label, px->tree);
    return 1;
}

static int _tag(NezParserContext *px, const char *tag)
{
    _log(px, OpTag, (void*)tag, NULL);
    return 1;
}

static int _value(NezParserContext *px, const char *text, size_t len)
{
    _log(px, OpReplace, (void*)text, (void*)len);
    return 1;
}

static int _fold(NezParserContext *px, int shift, const char* label)
{
    _log(px, OpNew, (void*)(px->pos + shift), NULL);
    _log(px, OpLink, (void*)label, px->tree);
    return 1;
}

static int _backL(NezParserContext *px, size_t treeLog)
{
    if (treeLog < px->treeLog) {
        size_t i;
        for(i = treeLog; i < px->treeLog; i++) {
            TreeLog *l = px->logs + i;
            l->op = 0;
            l->value = NULL;
            l->tree = NULL;
        }
        px->treeLog = treeLog;
    }
    return 1;
}

static int _EoT(NezParserContext *px, int shift, const char* tag, const char *text, size_t len)
{
    int objectSize = 0;
    size_t pos = 0;
    long i;
    for(i = px->treeLog - 1; i >= 0; i--) {
        TreeLog * l = px->logs + i;
        if(l->op == OpLink) {
            objectSize++;
            continue;
        }
        if(l->op == OpNew) {
			pos = (pchar *)l->value - px->inputs;
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
    //TreeLog * start = px->logs + i;
    if(text == NULL) {
    	len = ((px->pos + shift) - px->inputs) - pos;
    }
    px->tree = px->newfunc(tag, (const char*)px->inputs, pos, len, objectSize, text, px->thunk);
    if (objectSize > 0) {
        int n = 0;
        size_t j;
        for(j = i; j < px->treeLog; j++) {
            TreeLog * cur = px->logs + j;
            if (cur->op == OpLink) {
                px->tree = px->setfunc(px->tree, n++, (const char*)cur->value, cur->tree, px->thunk);
            }
        }
    }
    _backL(px, i);
    return 1;
}

// Memotable ------------------------------------------------------------

static void initMemo(NezParserContext *px, int w, int n)
{
    int i;
    if(n > 0) {
	    px->memoSize = w * n + 1;
    	px->memoArray = (MemoEntry *)_calloc(sizeof(MemoEntry), px->memoSize);
    	for (i = 0; i < px->memoSize; i++) {
        	px->memoArray[i].key = -1LL;
    	}
    }
}

static inline ukey_t longkey(ukey_t pos, int memoPoint) {
    return ((pos << 16) | memoPoint);
}

static int memoLookup(NezParserContext *px, int memoPoint)
{
    ukey_t key = longkey((px->pos - px->inputs), memoPoint);
    size_t hash = (size_t) (key % px->memoSize);
    MemoEntry* m = px->memoArray + hash;
    if (m->key == key && m->state == px->state) {
        px->pos += m->consumed;
        return m->result;
    }
    return NotFound;
}

static int memoLookupTree(NezParserContext *px, int memoPoint)
{
    ukey_t key = longkey((px->pos - px->inputs), memoPoint);
    size_t hash = (size_t) (key % px->memoSize);
    MemoEntry* m = px->memoArray + hash;
    if (m->key == key && m->state == px->state) {
        px->pos += m->consumed;
        px->tree = m->memoTree;
        return m->result;
    }
    return NotFound;
}

static int memoSucc(NezParserContext *px, int memoPoint, pchar* ppos)
{
    ukey_t key = longkey((ppos - px->inputs), memoPoint);
    size_t hash = (size_t) (key % px->memoSize);
    MemoEntry* m = px->memoArray + hash;
    m->key = key;
    m->memoTree = px->tree;
    m->consumed = px->pos - ppos;
    m->state = px->state;
    m->result = SuccFound;
    return 1;
}

static int memoSuccTree(NezParserContext *px, int memoPoint, pchar* ppos)
{
    ukey_t key = longkey((ppos - px->inputs), memoPoint);
    size_t hash = (size_t) (key % px->memoSize);
    MemoEntry* m = px->memoArray + hash;
    m->key = key;
    m->memoTree = px->tree;
    m->consumed = px->pos - ppos;
    m->result = SuccFound;
    m->state = px->state;
    return 1;
}

static int memoFail(NezParserContext *px, int memoPoint, pchar* ppos)
{
    ukey_t key = longkey((ppos - px->inputs), memoPoint);
    size_t hash = (size_t) (key % px->memoSize);
    MemoEntry* m = px->memoArray + hash;
    m->key = key;
    m->memoTree = px->tree;
    m->consumed = 0;
    m->result = FailFound;
    m->state = px->state;
    return 0;
}

//----------------------------------------------------------------------------

static inline int _bitis(int *bits, size_t n)
{
    return (bits[n / 32] & (1 << (n % 32))) != 0;
}

//----------------------------------------------------------------------------

typedef int (*ParserFunc)(NezParserContext *); 

/* Combinator */

static int pOption(NezParserContext *px, ParserFunc fmatch) {
	pchar *pos = px->pos;
	if (!fmatch(px)) {
		px->pos = pos;
	}
	return 1;
}

static int pOptionT(NezParserContext *px, ParserFunc fmatch) {
	pchar *pos = px->pos;
	void *tree = px->tree;
	size_t treeLog = px->treeLog;
	if (!fmatch(px)) {
		px->pos = pos;
		px->tree = tree;
		px->treeLog = treeLog;
	}
	return 1;
}

static int pOptionTS(NezParserContext *px, ParserFunc fmatch) {
	pchar *pos = px->pos;
	void *tree = px->tree;
	size_t treeLog = px->treeLog;
	void * state = px->state;
	if (!fmatch(px)) {
		px->pos = pos;
		px->tree = tree;
		px->treeLog = treeLog;
		px->state = state;
	}
	return 1;
}

static int pMany(NezParserContext *px, ParserFunc fmatch) {
	pchar *pos = px->pos;
	while (fmatch(px) && pos < px->pos) {
		pos = px->pos;
	}
	px->pos = pos;
	return 1;
}

static int pManyT(NezParserContext *px, ParserFunc fmatch) {
	pchar *pos = px->pos;
	void *tree = px->tree;
	size_t treeLog = px->treeLog;
	while (fmatch(px) && pos < px->pos) {
		pos = px->pos;
		tree = px->tree;
		treeLog = px->treeLog;
	}
	px->pos = pos;
	px->tree = tree;
	px->treeLog = treeLog;
	return 1;
}

static int pManyTS(NezParserContext *px, ParserFunc fmatch) {
	pchar *pos = px->pos;
	void *tree = px->tree;
	size_t treeLog = px->treeLog;
	void * state = px->state;
	while (fmatch(px) && pos < px->pos) {
		pos = px->pos;
		tree = px->tree;
		treeLog = px->treeLog;
		state = px->state;
	}
	px->pos = pos;
	px->tree = tree;
	px->treeLog = treeLog;
	px->state = state;
	return 1;
}

static int pAnd(NezParserContext *px, ParserFunc fmatch) {
	pchar *pos = px->pos;
	if (fmatch(px)) {
		px->pos = pos;
		return 1;
	}
	return 0;
}

static int pNot(NezParserContext *px, ParserFunc fmatch) {
	pchar *pos = px->pos;
	if (fmatch(px)) {
		return 0;
	}
	px->pos = pos;
	return 1;
}

static int pNotT(NezParserContext *px, ParserFunc fmatch) {
	pchar *pos = px->pos;
	void *tree = px->tree;
	size_t treeLog = px->treeLog;
	if (fmatch(px)) {
		return 0;
	}
	px->pos = pos;
	px->tree = tree;
	px->treeLog = treeLog;
	return 1;
}

static int pNotTS(NezParserContext *px, ParserFunc fmatch) {
	pchar *pos = px->pos;
	void *tree = px->tree;
	size_t treeLog = px->treeLog;
	void * state = px->state;
	if (fmatch(px)) {
		return 0;
	}
	px->pos = pos;
	px->tree = tree;
	px->treeLog = treeLog;
	px->state = state;
	return 1;
}

static int pLink(NezParserContext *px, ParserFunc fmatch, const char *label) {
	void *tree = px->tree;
	size_t treeLog = px->treeLog;
	if (!fmatch(px)) {
		return 0;
	}
	px->treeLog = treeLog;
	_link(px, label);
	px->tree = tree;
	return 1;
}

static int pMemo(NezParserContext *px, ParserFunc fmatch, int mp) {
	pchar *pos = px->pos;
	switch (memoLookup(px, mp)) {
	case 0:
		return (fmatch(px) && memoSucc(px, mp, pos)) || (memoFail(px, mp, pos));
	case 1:
		return 1;
	default:
		return 0;
	}
}

static int pMemoT(NezParserContext *px, ParserFunc fmatch, int mp) {
	pchar *pos = px->pos;
	switch (memoLookupTree(px, mp)) {
	case 0:
		return (fmatch(px) && memoSuccTree(px, mp, pos)) || (memoFail(px, mp, pos));
	case 1:
		return 1;
	default:
		return 0;
	}
}

/* Tree */

typedef struct Tree {
    const char             *key;
    int                     pos;
    int                     size;
	void                   *value;
} Tree;

static void *Tree_new(const char *tag, const char *inputs, size_t pos, size_t len, size_t nsubs, const char *value, void *thunk)
{
    Tree *t = (Tree*)_malloc(sizeof(struct Tree));
    t->key  = tag;
    t->pos  = pos;
    if(nsubs == 0) {
	    t->size = (int)(-len);
		t->value = (void *)inputs;
    }
    else {
    	t->size = nsubs;
    	t->value = _calloc(nsubs, sizeof(Tree));
    }
    return t;
}

static void* Tree_set(void *parent, size_t n, const char *label, void *child, void *thunk)
{
    Tree *t = (Tree*)parent;
    assert(t->size > 0);
    Tree *sub = (Tree *)t->value;
	sub[n].key = label;
	sub[n].pos = n;
	sub[n].value = child;
	return parent;
}

static void Tree_dump(Tree *t, FILE *fp)
{
    size_t i;
    if(t == NULL) {
        fputs("null", fp);
        return;
    }
    fputs("[#", fp);
    fputs(t->key != NULL ? t->key : "", fp);
    if(t->size <= 0) {
    	const char *text = t->value;
        fputs(" '", fp);
        for(i = 0; i < -(t->size); i++) {
            fputc(text[i], fp);
        }
        fputs("'", fp);
    }
    else {
    	Tree *sub = t->value;
        for(i = 0; i < t->size; i++) {
            fputs(" ", fp);
            fputs("$", fp);
            fputs(sub[i].key != NULL ? sub[i].key : "", fp);
            fputs("=", fp);
            Tree_dump(sub[i].value, fp);
        }
    }
    fputs("]", fp);
}

static void Tree_free(Tree *t)
{
    if(t != NULL) {
        if(t->size > 0) {
            size_t i = 0;
    		Tree *sub = t->value;
            for(i = 0; i < t->size; i++) {
                Tree_free((Tree*)sub[i].value);
            }
            _free(t->value);
        }
        _free(t);
    }
}


#define _ADHOC_MARK   (((size_t)1) << ((sizeof(void*)*8)-1))

/******
static void _pushGCBuf(NezParserContext *px, Tree *t)
{
    if(px->gcbuf_size == px->gcbuf_unused) {
        Tree **newbuf = (Tree **)_calloc(px->gcbuf_size * 2, sizeof(Tree *));
        memcpy(newbuf, px->gcbuf, sizeof(Tree *) * px->gcbuf_size);
        _free(px->gcbuf);
        px->gcbuf = newbuf;
        px->gcbuf_size *= 2;
    }
    px->gcbuf[px->gcbuf_size] = t;
    t->len |= _ADHOC_MARK;
    px->gcbuf_size++;
}


static Tree *Nez_token(NezParserContext *px)
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
        t->len = t->len | ~(_ADHOC_MARK);
    }
}

static void _SWEEP(NezParserContext *px)
{
    size_t i;
    for(i = 0; px->gcbuf_size; i++) {
        Tree *t = px->gcbuf[i];
        if((t->len & _ADHOC_MARK) == _ADHOC_MARK) {
            if(t->size > 0) {
                size_t i = 0;
                _free(t->labels);
                _free(t->childs);
            }
            _free(t);
        }
        px->gcbuf[i] = NULL;
    }
    px->gcbuf_size = 0;
}

static Tree* Nez_getTree(NezParserContext *px)
{
    _MARK(px->tree);
    _SWEEP(c);
    return px->tree;
}

****************************/

static void NezParserContext_free(NezParserContext *px)
{
    if(px->memoArray != NULL) {
        _free(px->memoArray);
        px->memoArray = NULL;
    }
    //Nez_backTreeLog(c, 0);
    _free(px->logs);
    px->logs = NULL;
    //_free(px->gcbuf);
    //px->gcbuf = NULL;
    _free(px);
}

/* lexer */

static int pOptionB(NezParserContext *px, pchar uchar) {
	if (_getbyte(px) == uchar) {
		_move(px,1);
	}
	return 1;
}

static int pOptionC(NezParserContext *px, int bools[]) {
	if (_bitis(bools, _getbyte(px))) {
		_move(px, 1);
	}
	return 1;
}

static int pOptionM(NezParserContext *px, const char *text, size_t len) {
	_matchBytes(px, text, len);
	return 1;
}

static int pManyB(NezParserContext *px, pchar uchar) {
	while (_getbyte(px) == uchar) {
		_move(px, 1);
	}
	return 1;
}

static int pManyC(NezParserContext *px, int bools[]) {
	while (_bitis(bools, _getbyte(px))) {
		_move(px, 1);
	}
	return 1;
}

static int pManyM(NezParserContext *px, const char *text, size_t len) {
	while (_matchBytes(px, text, len)) {
	}
	return 1;
}

static int pAndB(NezParserContext *px, pchar uchar) {
	return (_getbyte(px) == uchar);
}

static int pAndC(NezParserContext *px, int bools[]) {
	return _bitis(bools, _getbyte(px));
}

static int pAndM(NezParserContext *px, const char *text, size_t len) {
	pchar *pos = px->pos;
	int b = _matchBytes(px, text, len);
	if (b) {
		px->pos = pos;
	}
	return b;
}

static int pNotB(NezParserContext *px, pchar uchar) {
	return (_getbyte(px) != uchar);
}

static int pNotC(NezParserContext *px, int bools[]) {
	return !_bitis(bools, _getbyte(px));
}

static int pNotM(NezParserContext *px, const char *text, size_t len) {
	return !_matchBytes(px, text, len);
}






