#!/bin/sh
# easy install script

mvn package

if [ -e target/origami-*run.jar ]; then
	sudo cp target/origami-*run.jar /usr/local/lib/origami.jar
	# origami
	sudo cat << 'EOF' > /usr/local/bin/origami
#!/bin/sh
CLICOLOR=1 exec java -ea -jar /usr/local/lib/origami.jar $@

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


