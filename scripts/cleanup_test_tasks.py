"""
清理测试产生的废弃任务及其关联数据
删除目标：
  2037777386195402753 — 扩展字段端到端测试任务（status=2，已关闭）
  2037771806626041858 — 配置化测试（草稿，status=0）
"""
import psycopg2

TASK_IDS = [2037777386195402753, 2037771806626041858]

conn = psycopg2.connect(
    host='119.167.165.27', port=5432, dbname='zjylzl',
    user='postgres', password='zjylzl',
    options='-c search_path=zjylzl'
)
cur = conn.cursor()

for task_id in TASK_IDS:
    print(f"\n=== 清理 task_id={task_id} ===")

    # 1. 找到关联的 wr_record id 列表
    cur.execute("SELECT id FROM wr_record WHERE task_id = %s", (task_id,))
    record_ids = [row[0] for row in cur.fetchall()]
    print(f"  关联 record 数量: {len(record_ids)}")

    for rid in record_ids:
        # 2. 找到多条子记录 id（会议/培训/指导/调研/加分项）
        cur.execute("SELECT id FROM dw_meeting  WHERE record_id = %s", (rid,))
        sub_ids = [r[0] for r in cur.fetchall()]
        cur.execute("SELECT id FROM dw_training WHERE record_id = %s", (rid,))
        sub_ids += [r[0] for r in cur.fetchall()]
        cur.execute("SELECT id FROM dw_guidance WHERE record_id = %s", (rid,))
        sub_ids += [r[0] for r in cur.fetchall()]
        cur.execute("SELECT id FROM dw_survey   WHERE record_id = %s", (rid,))
        sub_ids += [r[0] for r in cur.fetchall()]
        cur.execute("SELECT id FROM dw_bonus    WHERE record_id = %s", (rid,))
        sub_ids += [r[0] for r in cur.fetchall()]

        # 3. 删除扩展字段值
        n = cur.execute("DELETE FROM dw_field_value WHERE record_id = %s", (rid,))
        cur.execute("DELETE FROM dw_field_value WHERE record_id = %s", (rid,))
        print(f"  record={rid}: dw_field_value 已删")

        # 4. 删除附件
        cur.execute("DELETE FROM dw_attachment WHERE record_id = %s", (rid,))
        print(f"  record={rid}: dw_attachment 已删")

        # 5. 删除子记录
        cur.execute("DELETE FROM dw_meeting  WHERE record_id = %s", (rid,))
        cur.execute("DELETE FROM dw_training WHERE record_id = %s", (rid,))
        cur.execute("DELETE FROM dw_guidance WHERE record_id = %s", (rid,))
        cur.execute("DELETE FROM dw_survey   WHERE record_id = %s", (rid,))
        cur.execute("DELETE FROM dw_funding  WHERE record_id = %s", (rid,))
        cur.execute("DELETE FROM dw_bonus    WHERE record_id = %s", (rid,))
        print(f"  record={rid}: dw_* 子表已删")

        # 6. 删除 wr_record_value（普通模板值，兼容）
        cur.execute("DELETE FROM wr_record_value WHERE record_id = %s", (rid,))

        # 7. 删除 wr_record
        cur.execute("DELETE FROM wr_record WHERE id = %s", (rid,))
        print(f"  record={rid}: wr_record 已删")

    # 8. 删除任务范围
    cur.execute("DELETE FROM wr_task_org_scope WHERE task_id = %s", (task_id,))
    print(f"  wr_task_org_scope 已删")

    # 9. 删除任务本身
    cur.execute("DELETE FROM wr_task WHERE id = %s", (task_id,))
    print(f"  wr_task 已删")

conn.commit()
print("\n=== 清理完成 ===")
conn.close()
