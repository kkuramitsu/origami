#include<stdio.h>
#include<stdlib.h>
#include<string.h>
#include<assert.h>

struct MemoEntry;
struct NezParserContext;

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


/* NezParserContext */

//static void *Tree_new(const char *tag, const char *inputs, const char *pos, const char *epos, size_t nsubs);
//static void* Tree_set(void *parent, size_t n, const char *label, void *child);
//static void initMemo(NezParserContext *px, int w, int n);

/*memo*/
static int initMemo(struct NezParserContext* px) {
  int cnt = 0;
  px->memos = (struct MemoEntry *)_calloc(sizeof(struct MemoEntry), 257);
  while(cnt < 257) {
    px->memos[cnt].key = -1LL;
    cnt = cnt + 1;
  }
  return 1;
}

static struct MemoEntry* getMemo(struct NezParserContext* px, unsigned long long int key) {
  return px->memos + (size_t)(key % 257);
}

/* API */

static inline char getbyte(struct NezParserContext* px) {
  return *(px->pos);
}
static inline char nextbyte(struct NezParserContext* px) {
  return return *(px->pos++);
}
static inline int eof(struct NezParserContext* px) {
  return !(px->pos < (px->inputs + px->length));
}

static inline int bitis(int *bits, size_t n)
{
  return (bits[n / 32] & (1 << (n % 32))) != 0;
}
static int matchBytes(NezParserContext *px, const char *text, size_t len) {
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

//----------------------------------------------------------------------------

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

****************************/





