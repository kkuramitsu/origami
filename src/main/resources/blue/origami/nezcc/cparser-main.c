/*memo*/

static int initMemo(struct NezParserContext* px) {
  int cnt = 0;
  px->memos = (struct MemoEntry *)_calloc(sizeof(struct MemoEntry), MEMOS);
  while(cnt < MEMOS) {
    px->memos[cnt].key = -1LL;
    cnt = cnt + 1;
  }
  return 1;
}

static unsigned long long int longkey(const char *pos, int memoPoint) {
  unsigned long long int key = (unsigned long long int)pos;
  return key * MEMOSIZE + memoPoint;
}

static struct MemoEntry* getMemo(struct NezParserContext* px, unsigned long long int key) {
  return px->memos + (((size_t)key % MEMOS));
}

/* API */

static inline char getbyte(struct NezParserContext* px) {
  return *(px->pos);
}
static inline char nextbyte(struct NezParserContext* px) {
  return *(px->pos++);
}
static inline int eof(struct NezParserContext* px) {
  return !(px->pos < (px->inputs + px->length));
}

static inline int bitis(const int *bits, size_t n)
{
  return (bits[n / 32] & (1 << (n % 32))) != 0;
}
static int matchBytes(struct NezParserContext *px, const char *text, size_t len) {
    if (px->pos + len > px->inputs + px->length) {
        return 0;
    }
    size_t i;
    for (i = 0; i < len; i++) {
        if (text[i] != px->pos[i]) {
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
    int                     size;
	void                   *value;
} Tree;

static void *Tree_new(const char *tag, const char *inputs, const char *pos, const char *epos, int nsubs)
{
    Tree *t = (Tree*)_malloc(sizeof(struct Tree));
    t->key  = tag;
    if(nsubs == 0) {
		const char *p = pos == NULL ? (void *)inputs : (void *)pos;
	    t->size = (int)(-(epos-p));
		t->value = (void*)p;
    }
    else {
    	t->size = nsubs;
    	t->value = _calloc(nsubs, sizeof(Tree));
    }
    return t;
}

static void* Tree_set(void *parent, int n, const char *label, void *child)
{
    Tree *t = (Tree*)parent;
    assert(t->size > 0);
    Tree *sub = (Tree *)t->value;
	sub[n].key = label;
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

static void TreeLog_free(struct TreeLog *t)
{
  while(t != NULL) {
    struct TreeLog *next = t->prevLog;
    _free(t);
    t = next;    
  }
}

static void NezParserContext_free(struct NezParserContext *px)
{
  if(px->memos != NULL) {
    _free(px->memos);
    px->memos = NULL;
  }
  TreeLog_free(px->treeLog);
  TreeLog_free(px->uLog);
  _free(px);
}

void *Nez_parse(const char *inputs, TreeFunc fnew, TreeSetFunc fset)
{
  struct NezParserContext *px = newNezParserContext(inputs, strlen(inputs), fnew, fset);
  px->pos = inputs;
  initMemo(px);
  void *result = NULL;
  if(e0(px)) {
    result = px->tree;
  }
  NezParserContext_free(px);
  return result;
}


Tree *Nez_parseTree(const char *inputs)
{
  return (Tree *)Nez_parse(inputs, Tree_new, Tree_set);
}

const char *Nez_readInput(const char *path)
{
    FILE *fp = fopen(path, "rb");
    if(fp != NULL) {
        size_t len;
        fseek(fp, 0, SEEK_END);
        len = (size_t) ftell(fp);
        fseek(fp, 0, SEEK_SET);
        char *buf = (char *) _calloc(1, len + 1);
        size_t readed = fread(buf, 1, len, fp);
        if(readed != len) {
            fprintf(stderr, "read error: %s\n", path);
            exit(1);
        }
        fclose(fp);
        return (const char*)buf;
    }
    return path;
}

int main(int ac, const char **av)
{
	int j;
	size_t len;
	if(ac == 1) {
		fprintf(stdout, "Usage: %s file [or input-text]\n", av[0]);
		return 1;
	}
	for(j = 1; j < ac; j++) {
		const char *inputs = Nez_readInput(av[j]);
        Tree *data = Nez_parseTree(inputs);
        fprintf(stdout, "%s: ", av[j]);
        Tree_dump(data, stdout);
        fprintf(stdout, "\n");
        Tree_free(data);
        if(av[j] != inputs) {
          _free((void*)inputs);
        }
	}
	return 0;
}

