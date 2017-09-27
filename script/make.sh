#!/bin/sh
# easy install script

NUM=`git log --oneline | wc -l | sed -e "s/[ \t]*//g"`
echo $NUM
cat << EOS > src/main/java/blue/origami/PatchLevel.java
package blue.origami;

public class PatchLevel {
        public final static int REV=$NUM;
}
EOS

if [ -d target ]; then
	rm -rf target
fi

mvn package

if [ -e target/origami-*run.jar ]; then
	sudo cp target/origami-*run.jar /usr/local/lib/origami.jar
	# origami
	sudo cat << 'EOF' > /usr/local/bin/origami
#!/bin/sh
CLICOLOR=1 exec java -ea -Duser.language=ja -Duser.country=JP -jar /usr/local/lib/origami.jar $@

EOF
	sudo chmod a+x /usr/local/bin/origami
	# inez
        sudo cat << 'EOF2' > /usr/local/bin/nez
#!/bin/sh
CLICOLOR=1 exec java -ea -jar /usr/local/lib/origami.jar $@

EOF2
        sudo chmod a+x /usr/local/bin/nez
	echo "Installed."
	echo "Try origami"
fi


