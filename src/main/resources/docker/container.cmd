export REPOSITORY_MYSQL=$repo_service &&
export REPOSITORY_MAIN=$repo_main &&
apt-get update &&
apt-get install -y wget &&
wget $repo_service/mariadb-template.sh --no-cache &&
chmod +x mariadb-template.sh &&
./mariadb-template.sh -d $database_name -p $database_password -e docker -l $log_host -m $log_port
