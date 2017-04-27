Tree *Nez_parse(const char *inputs, size_t len)
{
    NezParserContext *px = NezParserContext_new(inputs, len, MEMOSIZE);
    Tree *result = NULL;
    if(e0(px)) {
        result = (Tree *)px->tree;
    }
    NezParserContext_free(px);
    return result;
}

const char *Nez_readInput(const char *path, size_t *size)
{
    FILE *fp = fopen(path, "rb");
    if(fp != NULL) {
        size_t len;
        fseek(fp, 0, SEEK_END);
        len = (size_t) ftell(fp);
        fseek(fp, 0, SEEK_SET);
        char *buf = (char *) calloc(1, len + 1);
        size_t readed = fread(buf, 1, len, fp);
        if(readed != len) {
            fprintf(stderr, "read error: %s\n", path);
            exit(1);
        }
        fclose(fp);
        size[0] = len;
        return (const char*)buf;
    }
    size[0] = strlen(path);
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
		const char *inputs = Nez_readInput(av[j], &len);
        Tree *data = Nez_parse(inputs, len);
        fprintf(stdout, "%s: ", av[j]);
        Tree_dump(data, stdout);
        fprintf(stdout, "\n");
        Tree_free(data);
        //free((void*)inputs);
	}
	return 0;
}

