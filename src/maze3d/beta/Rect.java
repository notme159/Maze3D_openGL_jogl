package maze3d.beta;

public class Rect {

	private Vec3D topLeft;
	private Vec3D botLeft;
	private Vec3D botRight;
	private Vec3D topRight;
	
	
	public Rect(Vec3D topLeft, Vec3D botLeft, Vec3D botRight, Vec3D topRight) {
		super();
		this.topLeft = topLeft;
		this.botLeft = botLeft;
		this.botRight = botRight;
		this.topRight = topRight;
	}
	
	public Vec3D getTopLeft() {
		return topLeft;
	}
	public Vec3D getBotLeft() {
		return botLeft;
	}
	public Vec3D getBotRight() {
		return botRight;
	}
	public Vec3D getTopRight() {
		return topRight;
	}
	
	
}
