#/bin/ksh
unset ANT_HOME CLASSPATH

if [ ! -f tools/lib/junit.jar ]; then
  cp lib/ext/junit.jar tools/lib/
fi

PATH=.:$(pwd)/tools/bin:$JAVA_HOME/bin:$PATH
export PATH

if [ ! -f ant.properties ]; then
  echo "Please copy ant.default.properties file to ant.properties"
  echo "and modify settings to match your environment."
  exit 1
fi


WL_HOME=$(tmp=$(cat ant.properties | grep weblogic.dir) && tmp=${tmp##* } && echo ${tmp##*=})
if [ -z "$WL_HOME" -o ! -d "$WL_HOME" ]; then
    echo "Unable to find BEA WebLogic"
    echo "Please modify ant.properties file."
    exit 1
fi

echo "WL_HOME=$WL_HOME"

# Grab some file descriptors.
if [ "`uname -s`" != "OSF1" ]; then
  maxfiles=`ulimit -H -n`
else
  maxfiles=`ulimit -n`
fi

if [ !$? -a "$maxfiles" != 1024 ]; then
  if [ "$maxfiles" = "unlimited" ]; then
    maxfiles=1025
  fi
  if [ "$maxfiles" -lt 1024 ]; then
    ulimit -n $maxfiles
  else
    ulimit -n 1024
  fi
fi

# Figure out how to use our shared libraries
case `uname -s` in
AIX)
  if [ -n "$LIBPATH" ]; then
    LIBPATH=$LIBPATH:$WL_HOME/server/lib/aix:$WL_HOME/server/lib/aix/oci817_8
  else
    LIBPATH=$WL_HOME/server/lib/aix:$WL_HOME/server/lib/aix/oci817_8
  fi
  JAVA_OPTIONS="-Djava.security.auth.login.config=$WL_HOME/server/lib/aix/.java.login.config $JAVA_OPTIONS"
  PATH=$WL_HOME/server/lib/aix:$PATH
  export LIBPATH PATH
  export AIXTHREAD_SCOPE=S
  export AIXTHREAD_MUTEX_DEBUG=OFF
  export AIXTHREAD_RWLOCK_DEBUG=OFF
  export AIXTHREAD_COND_DEBUG=OFF
  echo "LIBPATH=$LIBPATH"
;;
HP-UX)
  if [ -n "$SHLIB_PATH" ]; then
    SHLIB_PATH=$SHLIB_PATH:$WL_HOME/server/lib/hpux11:$WL_HOME/server/lib/hpux11/oci817_8
  else
    SHLIB_PATH=$WL_HOME/server/lib/hpux11:$WL_HOME/server/lib/hpux11/oci817_8
  fi
  PATH=$WL_HOME/server/lib/hpux11:$PATH
  export SHLIB_PATH PATH
  echo "SHLIB_PATH=$SHLIB_PATH"
;;
IRIX)
  if [ -n "$LD_LIBRARY_PATH" ]; then
    LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$WL_HOME/server/lib/irix
  else
    LD_LIBRARY_PATH=$WL_HOME/server/lib/irix
  fi
  PATH=$WL_HOME/server/lib/irix:$PATH
  export LD_LIBRARY_PATH PATH
  echo "LD_LIBRARY_PATH=$LD_LIBRARY_PATH"
;;
LINUX|Linux)
  arch=`uname -m`
  if [ -n "$LD_LIBRARY_PATH" ]; then
    LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$WL_HOME/server/lib/linux/$arch:$WL_HOME/server/lib/linux/$arch/oci817_8
  else
    LD_LIBRARY_PATH=$WL_HOME/server/lib/linux/$arch:$WL_HOME/server/lib/linux/$arch/oci817_8
  fi
  PATH=$WL_HOME/server/lib/linux:$PATH
  export LD_LIBRARY_PATH PATH
  echo "LD_LIBRARY_PATH=$LD_LIBRARY_PATH"
;;
OSF1)
  if [ -n "$LD_LIBRARY_PATH" ]; then
    LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$WL_HOME/server/lib/tru64unix:$WL_HOME/server/lib/tru64unix/oci817_8
  else
    LD_LIBRARY_PATH=$WL_HOME/server/lib/tru64unix:$WL_HOME/server/lib/tru64unix/oci817_8
  fi
  PATH=$WL_HOME/server/lib/tru64unix:$PATH
  export LD_LIBRARY_PATH PATH
  echo "LD_LIBRARY_PATH=$LD_LIBRARY_PATH"
;;
  SunOS)
  if [ -n "$LD_LIBRARY_PATH" ]; then
    LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$WL_HOME/server/lib/solaris:$WL_HOME/server/lib/solaris/oci817_8
  else
    LD_LIBRARY_PATH=$WL_HOME/server/lib/solaris:$WL_HOME/server/lib/solaris/oci817_8
  fi
  PATH=$WL_HOME/server/lib/solaris:$PATH
  export LD_LIBRARY_PATH PATH
  echo "LD_LIBRARY_PATH=$LD_LIBRARY_PATH"
  JAVA_OPTIONS="-hotspot $JAVA_OPTIONS"
;;
OS/390)
  JAVA_OPTIONS="-ms128m -mx128m -Xnoargsconversion -Dfile.encoding=ISO-8859-1 -Dweblogic.NativeIOEnabled=false"
  PATH=$WL_HOME/bin/os390:$PATH
  JAVACMD=javacmd
;;
*)
  echo "$0: Don't know how to set the shared library path for `uname -s`.  "
esac

ANT_HOME=$(pwd)/tools
export ANT_HOME

ant $*

