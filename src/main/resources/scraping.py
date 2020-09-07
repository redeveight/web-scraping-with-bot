from selenium import webdriver
import io
import os

file_path = os.getcwd() + "\\servers_payments.txt"
driver_path = os.getcwd() + "\\phantomjs.exe"
websites = [""]


def main():
    try:
        open(file_path, 'w+').close()
        for i in range(0, websites.__len__()):
            driver = webdriver.PhantomJS(driver_path)
            driver.set_window_size(1120, 550)
            try:
                driver.get(websites[i])
                html = driver.page_source
                driver.quit()
                payments = html[
                           html.find("<div class=\"payment-list\">"):
                           html.find("</div>\n\n\t<div class=\"modal\" data-id=\"paymodal\">")]
            except Exception:
                payments = "#scan_error"

            with io.open(file_path, "a", encoding="utf-8") as file:
                if i != websites.__len__() - 1:
                    file.write(payments + "\n")
                else:
                    file.write(payments)
    except Exception: exit(1)

if __name__ == '__main__':
    main()