import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MisCursosComponent } from './mis-cursos.component';

describe('MisCursosComponent', () => {
  let component: MisCursosComponent;
  let fixture: ComponentFixture<MisCursosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MisCursosComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(MisCursosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
