#!/usr/bin/env bash

startEnvironment() {

  minikube start --cpus=2 --memory=2g --disk-size=10g --driver=hyperkit --container-runtime=docker --mount --mount-string="$PWD/docker:/initdb" --addons=ingress-dns --extra-config=apiserver.service-node-port-range=5400-33000

  eval $(minikube docker-env)

  cat << EOF > $TMPDIR/minikube-profilename-test
domain test
nameserver $(minikube ip)
search_order 1
timeout 5
EOF

  sudo mv $TMPDIR/minikube-profilename-test /etc/resolver/minikube-profilename-test

  kubectl apply -f environment/postgres/postgres-pv.yaml

  kubectl apply -f environment/postgres/postgres-deployment.yaml
}

deleteEnvironment() {
  minikube delete --all
}

if [ $# -ne 1 ]; then
  echo "Incorrect number of input arguments: $0 $*"
  echo "Usage: $0 <operation>"
  echo "Example: $0 --start"
  exit 1
fi

if [[ $OSTYPE == 'darwin'* ]]; then
  :
elif [[ $OSTYPE == 'linux'* ]]; then
  echo 'Coming soon on linux' >&2
  exit 1
else
  echo 'Unsupported OS' >&2
  exit 1
fi

while (( "$#" )); do
  case "$1" in
    -s|--start)
      startEnvironment
      shift 1
      ;;
    -d|--delete)
      deleteEnvironment
      shift 1
      ;;
    *)
      echo "Error: Unknown operation $1" >&2
      exit 1
      ;;
  esac
done
