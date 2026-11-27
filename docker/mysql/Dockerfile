FROM ubuntu:22.04
ARG MYSQL_VERSION=5.6.30
ARG MYSQL_SHA256=2be9c06936a188f11a4d6c6de791697dc5fb8532a0db3e041b6bf9a2e0a0e7cf
RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl libaio1 libncurses5 libnuma1 perl gosu \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 999 mysql && useradd --uid 999 --gid mysql --no-create-home mysql
RUN curl -fL --retry 3 "https://cdn.mysql.com/archives/mysql-5.6/mysql-${MYSQL_VERSION}-linux-glibc2.5-x86_64.tar.gz" -o /tmp/mysql.tar.gz \
    && echo "${MYSQL_SHA256}  /tmp/mysql.tar.gz" | sha256sum -c - \
    && mkdir -p /opt/mysql /var/lib/mysql /run/mysqld \
    && tar -xzf /tmp/mysql.tar.gz -C /opt/mysql --strip-components=1 \
    && rm /tmp/mysql.tar.gz \
    && chown mysql:mysql /var/lib/mysql /run/mysqld \
    && /opt/mysql/bin/mysqld --version
ENV PATH="/opt/mysql/bin:${PATH}"
COPY docker/mysql/entrypoint.sh /usr/local/bin/coffee-mysql-entrypoint
RUN chmod +x /usr/local/bin/coffee-mysql-entrypoint
EXPOSE 3306
ENTRYPOINT ["coffee-mysql-entrypoint"]
CMD ["--character-set-server=utf8mb4", "--collation-server=utf8mb4_bin"]
