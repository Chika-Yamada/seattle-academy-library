package jp.co.seattle.library.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jp.co.seattle.library.dto.BookDetailsInfo;
import jp.co.seattle.library.service.BooksService;
import jp.co.seattle.library.service.ThumbnailService;

/**
 * Handles requests for the application home page.
 */
@Controller // APIの入り口
public class EditController {
	final static Logger logger = LoggerFactory.getLogger(AddBooksController.class);

	@Autowired
	private BooksService booksService;

	@Autowired
	private ThumbnailService thumbnailService;

	@RequestMapping(value = "/edit", method = RequestMethod.POST) // value＝actionで指定したパラメータ
	// RequestParamでname属性を取得
	public String login(Model model, int bookId) {
		model.addAttribute("bookDetailsInfo", booksService.getBookInfo(bookId));
		return "edit";
	}

	/**
	 * 書籍情報を登録する
	 * 
	 * @param locale    ロケール情報
	 * @param id        ID
	 * @param title     書籍名
	 * @param author    著者名
	 * @param publisher 出版社
	 * @param file      サムネイルファイル
	 * @param model     モデル
	 * @return 遷移先画面
	 */
	@Transactional
	@RequestMapping(value = "/editBook", method = RequestMethod.POST, produces = "text/plain;charset=utf-8")
	public String editBook(Locale locale, @RequestParam("bookId") int bookId, @RequestParam("title") String title,
			@RequestParam("author") String author, @RequestParam("publisher") String publisher,
			@RequestParam("publishDate") String publishDate, @RequestParam("ISBN") String ISBN,
			@RequestParam("description") String description, @RequestParam("thumbnail") MultipartFile file,
			Model model) {
		logger.info("Welcome insertBooks.java! The client locale is {}.", locale);

		BookDetailsInfo bookInfo = new BookDetailsInfo();
		bookInfo.setBookId(bookId);
		bookInfo.setTitle(title);
		bookInfo.setAuthor(author);
		bookInfo.setPublisher(publisher);
		bookInfo.setPublishDate(publishDate);
		bookInfo.setISBN(ISBN);
		bookInfo.setDescription(description);

		// クライアントのファイルシステムにある元のファイル名を設定する
		String thumbnail = file.getOriginalFilename();

		if (!file.isEmpty()) {
			try {
				// サムネイル画像をアップロード
				String fileName = thumbnailService.uploadThumbnail(thumbnail, file);
				// URLを取得
				String thumbnailUrl = thumbnailService.getURL(fileName);

				bookInfo.setThumbnailName(fileName);
				bookInfo.setThumbnailUrl(thumbnailUrl);

			} catch (Exception e) {

				// 異常終了時の処理
				logger.error("サムネイルアップロードでエラー発生", e);
				model.addAttribute("bookDetailsInfo", bookInfo);
				return "edit";
			}
		}

		List<String> list = new ArrayList<String>();
		if (title.equals("") || author.equals("") || publisher.equals("") || publishDate.length() == 0) {
			list.add("<p>必須項目を入力してください。</p>");
		}

		if (!(publishDate.matches("^[0-9]{8}"))) {
			list.add("<p>出版日は半角数字のYYYYMMDD形式で入力してください。</p>");

		}

		if (ISBN.length() != 0 && !(ISBN.matches("^[0-9]{10}|[0-9]{13}"))) {
			list.add("<p>ISBNの桁数または半角数字が正しくありません。</p>");
		}

		if (list.size() > 0) {
			bookInfo.setThumbnailName("null");
			bookInfo.setThumbnailUrl("null");
			model.addAttribute("addErrorMessage", list);
			model.addAttribute("bookDetailsInfo", bookInfo);
			return "edit";
		}

		// 編集した書籍情報を登録する
		booksService.updatebookInfo(bookInfo);

		model.addAttribute("bookDetailsInfo", booksService.getBookInfo(bookId));
		return "details";

	}

}