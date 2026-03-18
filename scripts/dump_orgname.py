import psycopg2
conn = psycopg2.connect(host="119.167.165.27", port=5432, dbname="zjylzl",
                        user="postgres", password="zjylzl", options="-c search_path=zjylzl")
c = conn.cursor()
c.execute("SELECT u.account, o.org_name FROM sys_user u JOIN hr_organization o ON o.org_id=u.org_id WHERE u.account='wr_org_b'")
r = c.fetchone()
with open("tmp_orgname.txt", "w", encoding="utf-8") as f:
    f.write(f"account={r[0]}\norgName={r[1]}\nrepr={repr(r[1])}\nlen={len(r[1])}\n")

# Also check items
c.execute("SELECT item_name FROM wr_template_item WHERE template_id=2000000000000002 AND del_flag=0 AND is_leaf=1 ORDER BY col_index")
items = [row[0] for row in c.fetchall()]
orgName = r[1]
with open("tmp_orgname.txt", "a", encoding="utf-8") as f:
    f.write("\n--- contains check ---\n")
    for name in items:
        m1 = orgName in name
        m2 = name in orgName
        if m1 or m2:
            f.write(f"MATCH: orgName={orgName!r} vs item={name!r} (orgInItem={m1}, itemInOrg={m2})\n")
    f.write("done\n")
conn.close()
print("written to tmp_orgname.txt")
