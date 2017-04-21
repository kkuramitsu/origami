
Tree *Nez_parse(const char *inputs, size_t len)
{
    Nez *px = Nez_new(inputs, len);
    Tree *result = NULL;
    if(e0(px)) {
        result = Nez_getTree(px);
        if(result == NULL) {
            result = Nez_token(px);
        }
    }
    Nez_free(px);
    return result;
}

void Nez_dumpTree(Tree *t, FILE *fp);
const char *Nez_readInput(const char *path, size_t *size);

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
        Nez_dumpTree(data, stdout);
        fprintf(stdout, "\n");
        Nez_freeTree(data);
        free((void*)inputs);
	}
	return 0;
}


void Nez_dumpTree(Tree *t, FILE *fp)
{
    size_t i;
    if(t == NULL) {
        fputs("null", fp);
        return;
    }
    fputs("[#", fp);
    fputs(t->tag != NULL ? t->tag : "", fp);
    if(t->size == 0) {
        fputs(" '", fp);
        for(i = 0; i < t->len; i++) {
            fputc(t->text[i], fp);
        }
        fputs("'", fp);
    }
    else {
        for(i = 0; i < t->size; i++) {
            fputs(" ", fp);
            if(t->labels[i] != 0) {
                fputs("$", fp);
                fputs(t->labels[i] != NULL ? t->labels[i] : "", fp);
                fputs("=", fp);
            }
            Nez_dumpTree(t->childs[i], fp);
        }
    }
    fputs("]", fp);
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
