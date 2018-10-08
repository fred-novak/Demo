package com.example.demo.model;

@Searchable

public class Book {

	private String id;// 编号

	private String title;// 标题

	private String author;// 作者

	private float price;// 价格

	public Book() {

	}

	public Book(String id, String title, String author, float price) {

		super();

		this.id = id;

		this.title = title;

		this.author = author;

		this.price = price;

	}

	@SearchableId

	public String getId() {

		return id;

	}

	@SearchableProperty(boost = 2.0F, index = Index.TOKENIZED, store = Store.YES)

	public String getTitle() {

		return title;

	}

	@SearchableProperty(index = Index.TOKENIZED, store = Store.YES)

	public String getAuthor() {

		return author;

	}

	@SearchableProperty(index = Index.NO, store = Store.YES)

	public float getPrice() {

		return price;

	}

	public void setId(String id) {

		this.id = id;

	}

	public void setTitle(String title) {

		this.title = title;

	}

	public void setAuthor(String author) {

		this.author = author;

	}

	public void setPrice(float price) {

		this.price = price;

	}

	@Override

	public String toString() {

		return "[" + id + "] " + title + " - " + author + " $ " + price;

	}

}
