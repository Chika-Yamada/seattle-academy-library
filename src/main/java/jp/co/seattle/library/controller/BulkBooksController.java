package jp.co.seattle.library.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jp.co.seattle.library.dto.BookDetailsInfo;
import jp.co.seattle.library.service.BooksService;

@Controller // APIの入り口
public class BulkBooksController {
	final static Logger logger = LoggerFactory.getLogger(BulkBooksController.class);

	@Autowired
	private BooksService booksService;

	@RequestMapping(value = "/bulkBook", method = RequestMethod.GET) // value＝actionで指定したパラメータ
	// RequestParamでname属性を取得
	public String login(Model model) {
		return "bulkBook";
	}

	@Transactional
	@RequestMapping(value = "/bulkResistBook", method = RequestMethod.POST)
	public String bulkBook(Locale locale, @RequestParam("file") MultipartFile file, Model model) {
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			int count = 0;

			if (!br.ready()) {
				model.addAttribute("bulkErrorMessage", "CSVに書籍情報がありません。");
				return "bulkBook";
			}

			List<Integer> errorlist = new ArrayList<Integer>();
			List<String[]> bookslist = new ArrayList<String[]>();
			while ((line = br.readLine()) != null) {
				count = count + 1;
				final String[] split = line.split(",", -1);

				if (StringUtils.isEmpty(split[0]) || StringUtils.isEmpty(split[1]) || StringUtils.isEmpty(split[2])
						|| StringUtils.isEmpty(split[3]) || !(split[3].matches("^[0-9]{8}")
								|| split[4].length() != 0 && !split[4].matches("^[0-9]{10}|[0-9]{13}"))) {
					errorlist.add(count);
				} else {
					bookslist.add(split);
				}

			}

			if (errorlist.size() > 0) {
				List<String> bulkErrorMessage = new ArrayList<String>();
				for (int i = 0; i < errorlist.size(); i++) {
					bulkErrorMessage.add(errorlist.get(i) + "行目の書籍登録でエラーが起きました。");
				}
				model.addAttribute("bulkErrorMessage", bulkErrorMessage);
				return "bulkBook";
			}

			for (int i = 0; i < bookslist.size(); i++) {
				String[] booklist = bookslist.get(i);

				BookDetailsInfo bookInfo = new BookDetailsInfo();
				bookInfo.setTitle(booklist[0]);
				bookInfo.setAuthor(booklist[1]);
				bookInfo.setPublisher(booklist[2]);
				bookInfo.setPublishDate(booklist[3]);
				bookInfo.setISBN(booklist[4]);
				bookInfo.setDescription(booklist[5]);

				booksService.registBook(bookInfo);
				model.addAttribute("resultMessage", "登録完了");

			}

		} catch (IOException e) {
			model.addAttribute("ファイルが読み込めません", e);
		}

		return "redirect:/home";
	}
}
