FROM cassandra:latest

COPY cassandra-entrypoint-wrap.sh /entrypoint-wrap.sh
ENTRYPOINT ["/entrypoint-wrap.sh"]
CMD ["cassandra", "-f"]