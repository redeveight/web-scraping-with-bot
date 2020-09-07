const Telegraf = require('telegraf')
const sqlite3 = require('sqlite3').verbose();
const moment = require('moment');
const fs = require('fs');

let db = new sqlite3.Database(':memory:');
var base_query = "SELECT server_name, SUM(price*amount) AS total FROM payments WHERE (scan_time BETWEEN 'today 00:00' AND 'today 23:59') GROUP BY server_name ORDER BY total DESC;";
var period_query = "SELECT server_name, SUM(price*amount) AS total FROM payments WHERE (scan_time BETWEEN 'start 00:00' AND 'end 23:59') GROUP BY server_name ORDER BY total DESC;";
var target_query = "SELECT server_name, SUM(price*amount) AS total FROM payments WHERE (scan_time BETWEEN 'set_date1 00:00' AND 'set_date2 23:59') GROUP BY server_name ORDER BY total DESC;";
var list_query = "SELECT server_name, SUM(amount) AS amount, SUM(price*amount) AS total, title FROM payments WHERE (scan_time BETWEEN 'start 00:00' AND 'end 23:59') GROUP BY server_name, title ORDER BY server_name, amount;";
const help = '\"s\" - статистика за текущие сутки;\n\"s#\" - статистика за определенное количество дней, где # это количество дней. Например, s10;\n\"YYYY-MM-DD\" - статистика за определенный день, где YYYY это год, MM - месяц, а DD - день. Например, 2019-12-31.';
const bot = new Telegraf('1050883697:AAGbFr37ipCHiFMP3nW4HNwzYrERiVxSwWM')

bot.start((ctx) => ctx.reply(help))
bot.help((ctx) => ctx.reply(help))

bot.hears(/^(s|S|с|С){1}[0-9]*$/, (ctx) => {
    try {
        if (ctx.message.text.length == 1) {
            var query = base_query.replace("today", moment().subtract(0, 'days').format("YYYY-MM-DD")).replace("today", moment().subtract(0, 'days').format("YYYY-MM-DD"));
            let result = "";
            let revenue = 0;
            let db = new sqlite3.Database('./payments.db', sqlite3.OPEN_READONLY, (err) => {
                if (err) {
                    ctx.reply("В данный момент БД не доступна. Повторите попытку через пару минут.");
                    console.error(err.message);
                } else {
                    db.all(query, [], (err, rows) => {
                        if (err) {
                          throw err;
                        }
                        rows.forEach((row) => {
                            var value = parseInt(row.total);
                            revenue += value;
                            var value9procent = parseInt(value - (value * 9 / 100));
                            var value29procent = parseInt(value - (value * 29 / 100));
                            result += (row.server_name + ": " + String(value29procent) + " (" + String(value9procent) + "/" + String(value) + ")\n");
                        });
                        var value9procent = parseInt(revenue - (revenue * 9 / 100));
                        var value29procent = parseInt(revenue - (revenue * 29 / 100));
                        if (result != "") {
                            result = ("Today\'s revenue: " + String(value29procent) + " (" + String(value9procent) + "/" + String(revenue) + ")\n\n") + result;
                        } else {
                            ctx.reply('Today\'s revenue: 0');
                        }
                        ctx.reply(result);
			db.close();
                    });
                }
            });
        } else {
            var days = parseInt(ctx.message.text.substring(1, ctx.message.text.length));
            var query = period_query.replace("start", moment().subtract(days, 'days').format("YYYY-MM-DD")).replace("end", moment().subtract(0, 'days').format("YYYY-MM-DD"));

            let result = "";
            let revenue = 0;
            let db = new sqlite3.Database('./payments.db', sqlite3.OPEN_READONLY, (err) => {
                if (err) {
                    ctx.reply("В данный момент БД не доступна. Повторите попытку через пару минут.");
                    console.error(err.message);
                } else {
                    db.all(query, [], (err, rows) => {
                        if (err) {
                          throw err;
                        }
                        rows.forEach((row) => {
                            var value = parseInt(row.total);
                            revenue += value;
                            var value9procent = parseInt(value - (value * 9 / 100));
                            var value29procent = parseInt(value - (value * 29 / 100));
                            result += (row.server_name + ": " + String(value29procent) + " (" + String(value9procent) + "/" + String(value) + ")\n");
                        });
                        var value9procent = parseInt(revenue - (revenue * 9 / 100));
                        var value29procent = parseInt(revenue - (revenue * 29 / 100));
                        if (result != "") {
                            result = ("Revenue: " + String(value29procent) + " (" + String(value9procent) + "/" + String(revenue) + ")\n\n") + result;
                        } else {
                            ctx.reply('Revenue: 0');
                        }
                        ctx.reply(result);
                        db.close();
                    });
                }
            });
        }
    } catch (error) {
        console.error(error.message);
    }
});

bot.hears(/^(l|L|л|Л){1}[0-9]*$/, (ctx) => {
    try {
        if (ctx.message.text.length == 1) {
            var query = list_query.replace("start", moment().subtract(0, 'days').format("YYYY-MM-DD")).replace("end", moment().subtract(0, 'days').format("YYYY-MM-DD"));

            let result = "";
            let db = new sqlite3.Database('./payments.db', sqlite3.OPEN_READONLY, (err) => {
                if (err) {
                    ctx.reply("В данный момент БД не доступна. Повторите попытку через пару минут.");
                    console.error(err.message);
                } else {
                    db.all(query, [], (err, rows) => {
                        if (err) {
                          throw err;
                        }
                        rows.forEach((row) => {
                            result += (row.server_name + ": " + row.amount + "   " + row.total + "   " + row.title + "\n");
                        });
                        if (result == "") {
                            ctx.reply('No Records Found');
                        }
			//ctx.reply(result);
                        
                        fs.writeFile('list_result.txt', result, 'utf8', function (err) {
                            if (err) return console.log(err);
                        });
                        ctx.telegram.sendDocument(ctx.from.id, {
                            source: "list_result.txt",
                            filename: 'list_result.txt'
                        }).catch(function(error){ console.log(error); });
                        db.close();
                    });		
                }
            });
        } else {
            var days = parseInt(ctx.message.text.substring(1, ctx.message.text.length));
            var query = list_query.replace("start", moment().subtract(days, 'days').format("YYYY-MM-DD")).replace("end", moment().subtract(0, 'days').format("YYYY-MM-DD"));

            let result = "";
            let db = new sqlite3.Database('./payments.db', sqlite3.OPEN_READONLY, (err) => {
                if (err) {
                    ctx.reply("В данный момент БД не доступна. Повторите попытку через пару минут.");
                    console.error(err.message);
                } else {
                    db.all(query, [], (err, rows) => {
                        if (err) {
                          throw err;
                        }
                        rows.forEach((row) => {
                            result += (row.server_name + ": " + row.amount + "   " + row.total + "   " + row.title + "\n");
                        });
                        if (result == "") {
                            ctx.reply('No Records Found');
                        }
                        //ctx.reply(result);

                        fs.writeFile('list_result.txt', result, 'utf8', function (err) {
                            if (err) return console.log(err);
                        });
                        ctx.telegram.sendDocument(ctx.from.id, {
                            source: "C:\\Users\\Administrator\\Documents\\Projects\\MinecraftHubble\\list_result.txt",
                            filename: 'list_result.txt'
                        }).catch(function(error){ console.log(error); });
                        db.close();
                    });
                }
            });
        }
    } catch (error) {
        console.error(error.message);
    }
});

bot.hears(/^[0-9]{4}-[0-9]{2}-[0-9]{2}$/, (ctx) => {
    var message = String(ctx.message.text);
    var days = parseInt(ctx.message.text.substring(1, message.length));
    var query = target_query.replace("set_date1", message).replace("set_date2", message);

    let result = "";
    let revenue = 0;
    let db = new sqlite3.Database('./payments.db', sqlite3.OPEN_READONLY, (err) => {
        if (err) {
            ctx.reply("В данный момент БД не доступна. Повторите попытку через пару минут.");
            console.error(err.message);
        } else {
            db.all(query, [], (err, rows) => {
                if (err) {
                    throw err;
                }
                rows.forEach((row) => {
                    var value = parseInt(row.total);
                    revenue += value;
                    var value9procent = parseInt(value - (value * 9 / 100));
                    var value29procent = parseInt(value - (value * 29 / 100));
                    result += (row.server_name + ": " + String(value29procent) + " (" + String(value9procent) + "/" + String(value) + ")\n");
                });
                var value9procent = parseInt(revenue - (revenue * 9 / 100));
                var value29procent = parseInt(revenue - (revenue * 29 / 100));
                if (result != "") {
                    result = ("Revenue: " + String(value29procent) + " (" + String(value9procent) + "/" + String(revenue) + ")\n\n") + result;
                } else {
                    ctx.reply('Revenue: 0');
                }
                ctx.reply(result);
                db.close();
            });
        }
    });
});

bot.command('get', (ctx) => {
    return ctx.telegram.sendDocument(ctx.from.id, {
        source: "payments.db",
        filename: 'payments.db'
     }).catch(function(error){ console.log(error); });
});

bot.launch()