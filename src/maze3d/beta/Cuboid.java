package maze3d.beta;

public class Cuboid {

	private Rect bottom;
	private Rect top;
	private Rect front;
	private Rect back;
	private Rect left;
	private Rect right;
	
	public Cuboid(Rect bottom, Rect top, Rect front, Rect back, Rect left, Rect right) {
		super();
		this.bottom = bottom;
		this.top = top;
		this.front = front;
		this.back = back;
		this.left = left;
		this.right = right;
	}

	public Rect getBottom() {
		return bottom;
	}

	public Rect getTop() {
		return top;
	}

	public Rect getFront() {
		return front;
	}

	public Rect getBack() {
		return back;
	}

	public Rect getLeft() {
		return left;
	}

	public Rect getRight() {
		return right;
	}
}