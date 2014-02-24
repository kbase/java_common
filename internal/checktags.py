import sys
import re

VALID_TAG = re.compile("\d+.\d+.\d+")

if __name__ == "__main__":
    tag = ""
    for t in sys.argv[1:]:
        if VALID_TAG.match(t):
            if tag != "":
                print "Two valid tags for this commit: {} {}".format(tag, t)
                sys.exit(1)
            tag = t
    print tag
