/* Combinator */

public static interface ParserFunc<T> {
	boolean match(NezParserContext<T> px);
}

static final <T> boolean pOption(NezParserContext<T> px, ParserFunc<T> f) {
	int pos = px.pos;
	if (!f.match(px)) {
		px.pos = pos;
	}
	return true;
}

static final <T> boolean pOptionT(NezParserContext<T> px, ParserFunc<T> f) {
	int pos = px.pos;
	T tree = px.tree;
	TreeLog<T> treeLog = px.treeLog;
	if (!f.match(px)) {
		px.pos = pos;
		px.tree = tree;
		px.treeLog = treeLog;
	}
	return true;
}

static final <T> boolean pOptionTS(NezParserContext<T> px, ParserFunc<T> f) {
	int pos = px.pos;
	T tree = px.tree;
	TreeLog<T> treeLog = px.treeLog;
	SymbolTable state = px.state;
	if (!f.match(px)) {
		px.pos = pos;
		px.tree = tree;
		px.treeLog = treeLog;
		px.state = state;
	}
	return true;
}

static final <T> boolean pMany(NezParserContext<T> px, ParserFunc<T> f) {
	int pos = px.pos;
	while (f.match(px) && pos < px.pos) {
		pos = px.pos;
	}
	px.pos = pos;
	return true;
}

static final <T> boolean pManyT(NezParserContext<T> px, ParserFunc<T> f) {
	int pos = px.pos;
	T tree = px.tree;
	TreeLog<T> treeLog = px.treeLog;
	while (f.match(px) && pos < px.pos) {
		pos = px.pos;
		tree = px.tree;
		treeLog = px.treeLog;
	}
	px.pos = pos;
	px.tree = tree;
	px.treeLog = treeLog;
	return true;
}

static final <T> boolean pManyTS(NezParserContext<T> px, ParserFunc<T> f) {
	int pos = px.pos;
	T tree = px.tree;
	TreeLog<T> treeLog = px.treeLog;
	SymbolTable state = px.state;
	while (f.match(px) && pos < px.pos) {
		pos = px.pos;
		tree = px.tree;
		treeLog = px.treeLog;
		state = px.state;
	}
	px.pos = pos;
	px.tree = tree;
	px.treeLog = treeLog;
	px.state = state;
	return true;
}

static final <T> boolean pAnd(NezParserContext<T> px, ParserFunc<T> f) {
	int pos = px.pos;
	if (f.match(px)) {
		px.pos = pos;
		return true;
	}
	return false;
}

static final <T> boolean pNot(NezParserContext<T> px, ParserFunc<T> f) {
	int pos = px.pos;
	if (f.match(px)) {
		return false;
	}
	px.pos = pos;
	return true;
}

static final <T> boolean pNotT(NezParserContext<T> px, ParserFunc<T> f) {
	int pos = px.pos;
	T tree = px.tree;
	TreeLog<T> treeLog = px.treeLog;
	if (f.match(px)) {
		return false;
	}
	px.pos = pos;
	px.tree = tree;
	px.treeLog = treeLog;
	return true;
}

static final <T> boolean pNotTS(NezParserContext<T> px, ParserFunc<T> f) {
	int pos = px.pos;
	T tree = px.tree;
	TreeLog<T> treeLog = px.treeLog;
	SymbolTable state = px.state;
	if (f.match(px)) {
		return false;
	}
	px.pos = pos;
	px.tree = tree;
	px.treeLog = treeLog;
	px.state = state;
	return true;
}

static final <T> boolean pLink(NezParserContext<T> px, ParserFunc<T> f, String label) {
	T tree = px.tree;
	TreeLog<T> treeLog = px.treeLog;
	if (!f.match(px)) {
		return false;
	}
	px.treeLog = treeLog;
	px.linkTree(label);
	px.tree = tree;
	return true;
}

static final <T> boolean pMemo(NezParserContext<T> px, ParserFunc<T> f, int mp) {
	int pos = px.pos;
	switch (px.memoLookup(mp)) {
	case 0:
		return (f.match(px) && px.memoSucc(mp, pos)) || (px.memoFail(mp, pos));
	case 1:
		return true;
	default:
		return false;
	}
}

static final <T> boolean pMemoT(NezParserContext<T> px, ParserFunc<T> f, int mp) {
	int pos = px.pos;
	switch (px.memoLookupTree(mp)) {
	case 0:
		return (f.match(px) && px.memoSuccTree(mp, pos)) || (px.memoFail(mp, pos));
	case 1:
		return true;
	default:
		return false;
	}
}

