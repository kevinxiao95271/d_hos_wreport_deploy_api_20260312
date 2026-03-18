import psycopg2, sys
sys.stdout.reconfigure(encoding='utf-8')
conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl', user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()
cur.execute("SELECT column_name FROM information_schema.columns WHERE table_schema='zjylzl' AND table_name='wr_template' ORDER BY ordinal_position")
print("cols:", [r[0] for r in cur.fetchall()])
cur.execute("SELECT t.id, t.task_name, tpl.id, tpl.template_name FROM wr_task t JOIN wr_template tpl ON t.template_id=tpl.id WHERE t.del_flag=0")
for row in cur.fetchall(): print(row)
conn.close()
