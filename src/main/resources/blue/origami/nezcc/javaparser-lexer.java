/* Lexer */

static final <T> boolean pOptionB(NezParserContext<T> px, int uchar) {
	if (px.getbyte() == uchar) {
		px.move(1);
	}
	return true;
}

static final <T> boolean pOptionC(NezParserContext<T> px, boolean bools[]) {
	if (bools[px.getbyte()]) {
		px.move(1);
	}
	return true;
}

static final <T> boolean pOptionM(NezParserContext<T> px, byte[] text) {
	px.matchBytes(text);
	return true;
}

static final <T> boolean pManyB(NezParserContext<T> px, int uchar) {
	while (px.getbyte() == uchar) {
		px.move(1);
	}
	return true;
}

static final <T> boolean pManyC(NezParserContext<T> px, boolean bools[]) {
	while (bools[px.getbyte()]) {
		px.move(1);
	}
	return true;
}

static final <T> boolean pManyM(NezParserContext<T> px, byte[] text) {
	while (px.matchBytes(text)) {
	}
	return true;
}

static final <T> boolean pAndB(NezParserContext<T> px, int uchar) {
	return (px.getbyte() == uchar);
}

static final <T> boolean pAndC(NezParserContext<T> px, boolean bools[]) {
	return bools[px.getbyte()];
}

static final <T> boolean pAndM(NezParserContext<T> px, byte[] text) {
	int pos = px.pos;
	boolean b = px.matchBytes(text);
	if (b) {
		px.pos = pos;
	}
	return b;
}

static final <T> boolean pNotB(NezParserContext<T> px, int uchar) {
	return (px.getbyte() != uchar);
}

static final <T> boolean pNotC(NezParserContext<T> px, boolean bools[]) {
	return !bools[px.getbyte()];
}

static final <T> boolean pNotM(NezParserContext<T> px, byte[] text) {
	return !px.matchBytes(text);
}
