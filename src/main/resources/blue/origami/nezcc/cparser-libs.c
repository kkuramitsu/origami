#include<stdio.h>
#include<stdlib.h>
#include<string.h>
#include<assert.h>
#ifndef _WIN32
#include<sys/time.h> // for using gettimeofday
#endif


struct MemoEntry;
struct NezParserContext;
static int initMemo(struct NezParserContext* px);
static unsigned long long int longkey(const unsigned char *pos, int memoPoint);
static struct MemoEntry* getMemo(struct NezParserContext* px, unsigned long long int key);
static inline unsigned char getbyte(struct NezParserContext* px);
static inline unsigned char nextbyte(struct NezParserContext* px);
static inline int neof(struct NezParserContext* px);
static inline int bitis(const int *bits, size_t n);
static int matchBytes(struct NezParserContext *px, const void *text, size_t len);

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

static int match2(const unsigned char *c1, const unsigned char *c2) {
	return c1[0] == c2[0] && c1[1] == c2[1];
}

static int match3(const unsigned char *c1, const unsigned char *c2) {
	return c1[0] == c2[0] && c1[1] == c2[1] && c1[2] == c2[2];
}

static int match4(const unsigned char *c1, const unsigned char *c2) {
	return c1[0] == c2[0] && c1[1] == c2[1] && c1[2] == c2[2] && c1[3] == c2[3];
}

static int match5(const unsigned char *c1, const unsigned char *c2) {
	return c1[0] == c2[0] && c1[1] == c2[1] && c1[2] == c2[2] && c1[3] == c2[3] && c1[4] == c2[4];
}

static int match6(const unsigned char *c1, const unsigned char *c2) {
	return c1[0] == c2[0] && c1[1] == c2[1] && c1[2] == c2[2] && c1[3] == c2[3] && c1[4] == c2[4] && c1[5] == c2[5];
}

static int match7(const unsigned char *c1, const unsigned char *c2) {
	return c1[0] == c2[0] && c1[1] == c2[1] && c1[2] == c2[2] && c1[3] == c2[3] && c1[4] == c2[4] && c1[5] == c2[5] && c1[6] == c2[6];
}

static int match8(const unsigned char *c1, const unsigned char *c2) {
	return c1[0] == c2[0] && c1[1] == c2[1] && c1[2] == c2[2] && c1[3] == c2[3] && c1[4] == c2[4] && c1[5] == c2[5] && c1[6] == c2[6] && c1[7] == c2[7];
}

